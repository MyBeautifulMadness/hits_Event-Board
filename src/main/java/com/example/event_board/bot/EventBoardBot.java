package com.example.event_board.bot;

import com.example.event_board.config.TelegramProperties;
import com.example.event_board.entity.Event;
import com.example.event_board.entity.EventRegistration;
import com.example.event_board.entity.User;
import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import com.example.event_board.repository.EventRegistrationRepository;
import com.example.event_board.repository.EventRepository;
import com.example.event_board.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler.handleMessage;

@Component
@RequiredArgsConstructor
public class EventBoardBot extends TelegramLongPollingBot {

    private final TelegramProperties props;
    private final WebClient backend;
    private final UserRepository users;
    private final EventRepository events;
    private final EventRegistrationRepository regs;

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override public String getBotUsername() { return props.getUsername(); }
    @Override public String getBotToken()    { return props.getToken(); }

    @Override
    public void onUpdateReceived(Update u) {
        try {
            if (u.hasMessage() && u.getMessage().hasText()) {
                handleMessage(u);
                return;
            }
            if (u.hasCallbackQuery()) {
                handleCallback(u);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Update u) throws Exception {
        String chatId = u.getMessage().getChatId().toString();
        String text   = u.getMessage().getText().trim();

        String cmd, args;
        int sp = text.indexOf(' ');
        if (sp > 0) {
            cmd  = text.substring(0, sp).toLowerCase();
            args = text.substring(sp + 1).trim();
        } else {
            cmd  = text.toLowerCase();
            args = "";
        }

        String msg;

        msg = """
              Команды:
              /login <username> <password> — авторизация
              /logout — выйти
              
              Команды студента:
              /events — список событий
              /my — активные записи
              
              Команды менеджера:
              /mevents — мои события
              /newevent "title" yyyy-MM-dd HH:mm yyyy-MM-dd HH:mm "location" "desc" - создать новое событие
              /editevent <id> (title|desc|loc|start|end) <значение> - отредактировать событие
              /setdeadline <id> yyyy-MM-dd HH:mm - установить дедлайн
              /attendees <id> - список записавшихся
              /delevent <id> - удалить событие
              """;

        switch (cmd) {

            case "/start" -> onStart(chatId);
            case "/login" -> onLogin(chatId, text);
            case "/logout" -> onLogout(chatId);

            case "/my" -> onMyEvents(chatId);
            case "/events" -> sendEvents(chatId);

            case "/mevents" -> onManagerEvents(chatId);
            case "/newevent" -> onNewEvent(chatId, "/newevent " + args);
            case "/editevent" -> onEditEvent(chatId, "/editevent " + args);
            case "/setdeadline"-> onSetDeadline(chatId, "/setdeadline " + args);
            case "/delevent" -> onDeleteEvent(chatId, "/delevent " + args);
            case "/attendees" -> onAttendees(chatId, "/attendees " + args);

            default -> send(chatId, msg);
        }
    }

    private void handleCallback(Update u) throws Exception {
        var chatId = u.getCallbackQuery().getMessage().getChatId().toString();
        var data   = u.getCallbackQuery().getData();

        if (data.startsWith("EVT_INFO_")) {
            long id = Long.parseLong(data.substring("EVT_INFO_".length()));
            sendEventDetails(chatId, id);
        } else if (data.startsWith("EVT_REG_")) {
            long id = Long.parseLong(data.substring("EVT_REG_".length()));
            onRegisterForEvent(chatId, id);
        }

        if (data.startsWith("MGR_ATT_")) {
            long id = Long.parseLong(data.substring("MGR_ATT_".length()));
            onAttendees(chatId, "/attendees " + id);
        } else if (data.startsWith("MGR_DEL_")) {
            long id = Long.parseLong(data.substring("MGR_DEL_".length()));
            onDeleteEvent(chatId, "/delevent " + id);
        }
    }

    private void onStart(String chatId) throws Exception {
        var u = users.findByTelegramId(chatId).orElse(null);
        if (u != null) {
            send(chatId, "Вы уже авторизованы как " + u.getUsername()
                    + " (статус: " + u.getStatus() + ").\nКоманды: /events");
            return;
        }
        send(chatId,
                "Привет! Чтобы привязать аккаунт, введите:\n" +
                        "/login <username> <password>\n\n" +
                        "После подтверждения деканатом студент сможет записываться на события.");
    }

    private void onLogin(String chatId, String text) throws Exception {
        String[] p = text.split("\\s+");
        if (p.length < 3) {
            send(chatId, "Использование: /login <username> <password>");
            return;
        }
        String username = p[1], password = p[2];

        try {
            var jwt = backend.post()
                    .uri("/api/auth/login")
                    .bodyValue(Map.of("username", username, "password", password))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(7));

            if (jwt == null || !jwt.containsKey("accessToken")) {
                send(chatId, "Неверный логин или пароль.");
                return;
            }

            var userOpt = users.findByUsername(username);
            if (userOpt.isEmpty()) {
                send(chatId, "Пользователь не найден в системе.");
                return;
            }
            var user = userOpt.get();

            users.findByTelegramId(chatId).ifPresent(other -> {
                if (!other.getId().equals(user.getId())) {
                    other.setTelegramId(null);
                    users.save(other);
                }
            });

            user.setTelegramId(chatId);
            users.save(user);

            send(chatId, "Авторизация успешна. Привет, " + user.getUsername()
                    + "! Статус: " + user.getStatus() + ". Просмотр команд: /start");
        } catch (Exception e) {
            send(chatId, "Не удалось авторизоваться. Проверьте логин/пароль.");
        }
    }

    private void onLogout(String chatId) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (me == null) { send(chatId, "Вы не авторизованы."); return; }
        me.setTelegramId(null);
        users.save(me);
        send(chatId, "Готово. Привязка Telegram снята.");
    }

    private void sendEvents(String chatId) throws Exception {
        var list = events.findAll();
        if (list.isEmpty()) { send(chatId, "Событий пока нет."); return; }

        StringBuilder sb = new StringBuilder("События:\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Event e : list) {
            sb.append("• ").append(e.getId()).append(": ").append(e.getTitle())
                    .append(" — ").append(fmt(e.getStartTime())).append(" → ").append(fmt(e.getEndTime()))
                    .append(" @ ").append(e.getLocation()==null? "-" : e.getLocation()).append("\n");
            rows.add(List.of(btn("Подробнее " + e.getId(), "EVT_INFO_" + e.getId())));
        }
        var kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        send(chatId, sb.toString(), kb);
    }

    private void sendEventDetails(String chatId, long eventId) throws Exception {
        var e = events.findById(eventId).orElse(null);
        if (e == null) { send(chatId, "Событие не найдено"); return; }

        var sb = new StringBuilder();
        sb.append("«").append(e.getTitle()).append("»\n");
        if (e.getCompany()!=null) sb.append("Компания: ").append(e.getCompany().getName()).append("\n");
        sb.append("Где: ").append(e.getLocation()==null? "-" : e.getLocation()).append("\n")
                .append("Когда: ").append(fmt(e.getStartTime())).append(" → ").append(fmt(e.getEndTime())).append("\n");
        if (e.getSignupDeadline()!=null) sb.append("Дедлайн записи: ").append(fmt(e.getSignupDeadline())).append("\n");
        if (e.getDescription()!=null) sb.append("\n").append(e.getDescription());

        var me = users.findByTelegramId(chatId).orElse(null);
        boolean can=false, already=false;
        if (me!=null && me.getRole()==UserRole.STUDENT && me.getStatus()==AccountStatus.APPROVED) {
            can = isOpen(e);
            already = regs.existsByEventIdAndStudentId(eventId, me.getId());
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (can && !already) rows.add(List.of(btn("Записаться", "EVT_REG_"+eventId)));

        var kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        send(chatId, sb.toString(), kb);
    }

    private void onRegisterForEvent(String chatId, long eventId) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (me==null) { send(chatId, "Сначала /start"); return; }
        if (!(me.getRole()==UserRole.STUDENT && me.getStatus()==AccountStatus.APPROVED)) {
            send(chatId, "Запись доступна только подтвержденным студентам."); return;
        }
        var e = events.findById(eventId).orElse(null);
        if (e==null) { send(chatId, "Событие не найдено"); return; }
        if (!isOpen(e)) { send(chatId, "Запись закрыта."); return; }
        if (regs.existsByEventIdAndStudentId(eventId, me.getId())) { send(chatId, "Вы уже записаны."); return; }

        EventRegistration r = new EventRegistration();
        r.setEvent(e);
        r.setStudent(me);
        regs.save(r);

        send(chatId, "Записал на «" + e.getTitle() + "».");
    }

    private void onMyEvents(String chatId) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (me == null) {
            send(chatId, "Сначала авторизуйтесь: /login <username> <password>");
            return;
        }
        if (me.getRole() != UserRole.STUDENT) {
            send(chatId, "Список личных записей доступен только студентам.");
            return;
        }

        var now = LocalDateTime.now();

        var active = regs.findByStudent_Id(me.getId()).stream()
                .map(EventRegistration::getEvent)
                .filter(Objects::nonNull)
                .filter(ev -> {
                    if (ev.getEndTime() != null) return now.isBefore(ev.getEndTime());
                    return ev.getStartTime() == null || now.isBefore(ev.getStartTime());
                })
                .sorted(Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (active.isEmpty()) {
            send(chatId, "Активных записей нет.");
            return;
        }

        StringBuilder sb = new StringBuilder("Ваши активные записи:\n\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Event e : active) {
            sb.append("• ").append(e.getId()).append(": ").append(e.getTitle()).append("\n")
                    .append("  ").append(fmt(e.getStartTime())).append(" → ").append(fmt(e.getEndTime())).append("\n")
                    .append("  ").append(e.getLocation() == null ? "-" : e.getLocation()).append("\n\n");

            rows.add(List.of(
                    btn("Подробнее " + e.getId(), "EVT_INFO_" + e.getId())
            ));
        }

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        send(chatId, sb.toString(), kb);
    }

    private void onManagerEvents(String chatId) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (!isManagerApproved(me)) {
            send(chatId, "Доступно только подтверждённым менеджерам.");
            return;
        }

        var list = events.findByCreatedBy_Id(me.getId());
        if (list.isEmpty()) { send(chatId, "У вас пока нет событий."); return; }

        StringBuilder sb = new StringBuilder("Мои события:\n\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Event e : list) {
            sb.append("• ").append(e.getId()).append(": ").append(e.getTitle())
                    .append("\n  ").append(fmt(e.getStartTime())).append(" → ").append(fmt(e.getEndTime()))
                    .append("\n  Место: ").append(e.getLocation()==null?"—":e.getLocation())
                    .append("\n  Дедлайн записи: ").append(fmt(e.getSignupDeadline()))
                    .append("\n\n");

            rows.add(List.of(
                    btn("Подробнее " + e.getId(), "EVT_INFO_" + e.getId()),
                    btn("Записавшиеся " + e.getId(), "MGR_ATT_" + e.getId()),
                    btn("Удалить " + e.getId(), "MGR_DEL_" + e.getId())
            ));
        }
        var kb = new InlineKeyboardMarkup(); kb.setKeyboard(rows);
        send(chatId, sb.toString(), kb);
    }

    private void onNewEvent(String chatId, String text) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (!isManagerApproved(me)) { send(chatId, "Доступно только менеджерам."); return; }

        var args = splitArgs(text);

        if (args.size() < 7) {
            send(chatId, "Формат:\n/newevent \"Заголовок\" yyyy-MM-dd HH:mm yyyy-MM-dd HH:mm \"Место\" [\"Описание\"]");
            return;
        }

        String title   = args.get(1);
        String startDt = args.get(2) + " " + args.get(3);
        String endDt   = args.get(4) + " " + args.get(5);
        String loc     = args.get(6);
        String desc    = (args.size() >= 8) ? args.get(7) : null;

        var e = new Event();
        e.setTitle(title);
        e.setStartTime(parseDt(startDt));
        e.setEndTime(parseDt(endDt));
        e.setLocation(loc);
        e.setDescription(desc);
        e.setCreatedBy(me);
        e.setCompany(me.getCompany());

        events.save(e);
        send(chatId, "Создано событие #" + e.getId() + " — «" + e.getTitle() + "».");
    }

    private void onEditEvent(String chatId, String text) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (!isManagerApproved(me)) { send(chatId, "Доступно только менеджерам."); return; }

        // /editevent <id> title|start|end|loc|desc <значение...>
        var parts = text.split("\\s+", 4);
        if (parts.length < 4) {
            send(chatId, "Формат:\n/editevent <id> title|start|end|loc|desc <значение>");
            return;
        }
        long id = Long.parseLong(parts[1]);
        String field = parts[2].toLowerCase();
        String value = parts[3];

        var e = events.findById(id).orElse(null);
        if (e==null || !Objects.equals(e.getCreatedBy().getId(), me.getId())) {
            send(chatId, "Событие не найдено или не принадлежит вам."); return;
        }

        switch (field) {
            case "title" -> e.setTitle(value);
            case "loc", "location" -> e.setLocation(value);
            case "desc", "description" -> e.setDescription(value);
            case "start" -> e.setStartTime(parseDt(value));          // ожидаем "yyyy-MM-dd HH:mm"
            case "end"   -> e.setEndTime(parseDt(value));
            default -> { send(chatId, "Поле: title|start|end|loc|desc"); return; }
        }

        events.save(e);
        send(chatId, "Обновлено событие #" + e.getId() + " (" + field + ").");
    }

    private void onSetDeadline(String chatId, String text) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (!isManagerApproved(me)) { send(chatId, "Доступно только менеджерам."); return; }

        var parts = text.split("\\s+", 3);
        if (parts.length < 3) {
            send(chatId, "Формат:\n/setdeadline <id> yyyy-MM-dd HH:mm");
            return;
        }
        long id = Long.parseLong(parts[1]);
        String dl = parts[2];

        var e = events.findById(id).orElse(null);
        if (e==null || !Objects.equals(e.getCreatedBy().getId(), me.getId())) {
            send(chatId, "Событие не найдено или не принадлежит вам."); return;
        }

        e.setSignupDeadline(parseDt(dl));
        events.save(e);
        send(chatId, "Дедлайн установлен: " + fmt(e.getSignupDeadline()));
    }

    private void onDeleteEvent(String chatId, String text) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (!isManagerApproved(me)) { send(chatId, "Доступно только менеджерам."); return; }

        var parts = text.split("\\s+");
        if (parts.length < 2) { send(chatId, "Формат: /delevent <id>"); return; }
        long id = Long.parseLong(parts[1]);

        var e = events.findById(id).orElse(null);
        if (e==null || !Objects.equals(e.getCreatedBy().getId(), me.getId())) {
            send(chatId, "Событие не найдено или не принадлежит вам."); return;
        }

        var regsToDelete = regs.findByEvent_Id(id);
        if (!regsToDelete.isEmpty()) regs.deleteAll(regsToDelete);

        events.delete(e);
        send(chatId, "Событие #" + id + " удалено.");
    }

    private void onAttendees(String chatId, String text) throws Exception {
        var me = users.findByTelegramId(chatId).orElse(null);
        if (!isManagerApproved(me)) { send(chatId, "Доступно только менеджерам."); return; }

        var parts = text.split("\\s+");
        if (parts.length < 2) { send(chatId, "Формат: /attendees <id>"); return; }
        long id = Long.parseLong(parts[1]);

        var e = events.findById(id).orElse(null);
        if (e==null || !Objects.equals(e.getCreatedBy().getId(), me.getId())) {
            send(chatId, "Событие не найдено или не принадлежит вам."); return;
        }

        var list = regs.findByEvent_Id(id);
        if (list.isEmpty()) { send(chatId, "На событие #" + id + " пока никто не записан."); return; }

        StringBuilder sb = new StringBuilder("Записавшиеся на «" + e.getTitle() + "»:\n\n");
        int i=1;
        for (var r : list) {
            var st = r.getStudent();
            sb.append(i++).append(". @").append(st.getUsername());
            if (st.getTelegramId()!=null) sb.append(" (").append(st.getTelegramId()).append(")");
            sb.append("\n");
        }
        send(chatId, sb.toString());
    }

    private String fmt(LocalDateTime dt){ return dt==null ? "-" : dt.format(F); }
    private boolean isOpen(Event e){
        var now = LocalDateTime.now();
        if (e.getStartTime()!=null && !now.isBefore(e.getStartTime())) return false;
        if (e.getSignupDeadline()!=null && now.isAfter(e.getSignupDeadline())) return false;
        return true;
    }
    private InlineKeyboardButton btn(String text, String data){
        var b = new InlineKeyboardButton(text);
        b.setCallbackData(data);
        return b;
    }
    private void send(String chatId, String text) throws Exception {
        execute(SendMessage.builder().chatId(chatId).text(text).build());
    }
    private void send(String chatId, String text, InlineKeyboardMarkup kb) throws Exception {
        execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(kb).build());
    }

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private LocalDateTime parseDt(String s) {
        return LocalDateTime.parse(s, DT);
    }

    private boolean isManagerApproved(User me) {
        return me != null && me.getRole() == UserRole.MANAGER && me.getStatus() == AccountStatus.APPROVED;
    }

    private List<String> splitArgs(String input) {
        List<String> out = new ArrayList<>();
        boolean inQ = false;
        StringBuilder b = new StringBuilder();
        for (int i=0;i<input.length();i++){
            char c = input.charAt(i);
            if (c=='"'){ inQ=!inQ; continue; }
            if (!inQ && Character.isWhitespace(c)){
                if (b.length()>0){ out.add(b.toString()); b.setLength(0); }
            } else {
                b.append(c);
            }
        }
        if (b.length()>0) out.add(b.toString());
        return out;
    }

}