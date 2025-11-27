package com.example.be.service.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Intern Management System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Tạo Google Calendar client từ access token của user
     */
    private Calendar getCalendarService(String accessToken) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Tạo credentials từ access token
        GoogleCredentials credentials = GoogleCredentials.create(
                new AccessToken(accessToken, null)
        );

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Tạo event mới trên Google Calendar
     */
    public String createEvent(
            String accessToken,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            String attendeeEmail
    ) throws Exception {

        Calendar service = getCalendarService(accessToken);

        Event event = new Event()
                .setSummary(title)
                .setDescription(description)
                .setLocation(location);

        // Set start time
        DateTime startDateTime = new DateTime(
                Date.from(startTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
        );
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Asia/Ho_Chi_Minh");
        event.setStart(start);

        // Set end time
        DateTime endDateTime = new DateTime(
                Date.from(endTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
        );
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Asia/Ho_Chi_Minh");
        event.setEnd(end);

        // Add attendee
        if (attendeeEmail != null && !attendeeEmail.isEmpty()) {
            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail(attendeeEmail)
            };
            event.setAttendees(Arrays.asList(attendees));
        }

        // Send notifications
        event.setReminders(new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(
                        new EventReminder().setMethod("email").setMinutes(24 * 60), // 1 day before
                        new EventReminder().setMethod("popup").setMinutes(30) // 30 mins before
                )));

        // Insert event
        String calendarId = "primary";
        event = service.events().insert(calendarId, event).setSendUpdates("all").execute();

        System.out.printf("✅ Event created: %s\n", event.getHtmlLink());

        return event.getId();
    }

    /**
     * Cập nhật event trên Google Calendar
     */
    public void updateEvent(
            String accessToken,
            String eventId,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location
    ) throws Exception {

        Calendar service = getCalendarService(accessToken);

        // Get existing event
        Event event = service.events().get("primary", eventId).execute();

        // Update fields
        event.setSummary(title);
        event.setDescription(description);
        event.setLocation(location);

        // Update start time
        DateTime startDateTime = new DateTime(
                Date.from(startTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
        );
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Asia/Ho_Chi_Minh");
        event.setStart(start);

        // Update end time
        DateTime endDateTime = new DateTime(
                Date.from(endTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
        );
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Asia/Ho_Chi_Minh");
        event.setEnd(end);

        // Update event
        service.events().update("primary", eventId, event).setSendUpdates("all").execute();

        System.out.printf("✅ Event updated: %s\n", event.getHtmlLink());
    }

    /**
     * Xóa event khỏi Google Calendar
     */
    public void deleteEvent(String accessToken, String eventId) throws Exception {
        Calendar service = getCalendarService(accessToken);
        service.events().delete("primary", eventId).setSendUpdates("all").execute();
        System.out.printf("✅ Event deleted: %s\n", eventId);
    }

    /**
     * Kiểm tra access token còn hợp lệ không
     */
    public boolean isTokenValid(String accessToken) {
        try {
            getCalendarService(accessToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}