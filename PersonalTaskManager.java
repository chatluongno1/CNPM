import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PersonalTaskManager {

    private static final String DB_FILE_PATH = "tasks_database.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> VALID_PRIORITIES = Arrays.asList("Thấp", "Trung bình", "Cao");
    
    private static int taskIdCounter = 1; // Simple ID generation

    // Utility method to load data
    private JSONArray loadTasks() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(DB_FILE_PATH)) {
            Object obj = parser.parse(reader);
            return (obj instanceof JSONArray) ? (JSONArray) obj : new JSONArray();
        } catch (IOException | ParseException e) {
            System.err.println("Lỗi khi đọc file database: " + e.getMessage());
            return new JSONArray();
        }
    }

    // Utility method to save data
    private void saveTasks(JSONArray tasks) {
        try (FileWriter file = new FileWriter(DB_FILE_PATH)) {
            file.write(tasks.toJSONString());
            file.flush();
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file database: " + e.getMessage());
        }
    }

    // Validation methods
    private boolean isValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private LocalDate parseDate(String dateStr) throws IllegalArgumentException {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày không hợp lệ. Sử dụng định dạng YYYY-MM-DD.");
        }
    }

    private boolean isValidPriority(String priority) {
        return VALID_PRIORITIES.contains(priority);
    }

    private boolean isDuplicateTask(JSONArray tasks, String title, String dueDateStr) {
        return tasks.stream()
                .map(obj -> (JSONObject) obj)
                .anyMatch(task -> task.get("title").toString().equalsIgnoreCase(title) &&
                         task.get("due_date").toString().equals(dueDateStr));
    }

    // Main method to add task
    public JSONObject addTask(String title, String description, String dueDateStr, String priority) {
        try {
            // Validate inputs
            validateInputs(title, dueDateStr, priority);
            
            // Parse date
            LocalDate dueDate = parseDate(dueDateStr);
            String formattedDate = dueDate.format(DATE_FORMATTER);
            
            // Load and check for duplicates
            JSONArray tasks = loadTasks();
            if (isDuplicateTask(tasks, title, formattedDate)) {
                throw new IllegalArgumentException(
                    String.format("Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
            }

            // Create and save new task
            JSONObject newTask = createTask(title, description, formattedDate, priority);
            tasks.add(newTask);
            saveTasks(tasks);

            System.out.println(String.format("Đã thêm nhiệm vụ mới thành công với ID: %d", 
                                            taskIdCounter - 1));
            return newTask;

        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi: " + e.getMessage());
            return null;
        }
    }

    private void validateInputs(String title, String dueDateStr, String priority) {
        if (!isValidString(title)) {
            throw new IllegalArgumentException("Tiêu đề không được để trống.");
        }
        if (!isValidString(dueDateStr)) {
            throw new IllegalArgumentException("Ngày đến hạn không được để trống.");
        }
        if (!isValidPriority(priority)) {
            throw new IllegalArgumentException("Mức độ ưu tiên không hợp lệ. Chọn từ: " + 
                                             String.join(", ", VALID_PRIORITIES));
        }
    }

    private JSONObject createTask(String title, String description, String dueDate, String priority) {
        JSONObject task = new JSONObject();
        task.put("id", taskIdCounter++);
        task.put("title", title);
        task.put("description", description);
        task.put("due_date", dueDate);
        task.put("priority", priority);
        task.put("status", "Chưa hoàn thành");
        return task;
    }

    // Chạy thử chương trình
    public static void main(String[] args) {
        PersonalTaskManager manager = new PersonalTaskManager();

        System.out.println("\nThêm nhiệm vụ hợp lệ:");
        manager.addTask("Mua sách", "Sách Công nghệ phần mềm.", "2025-07-20", "Cao");

        System.out.println("\nThêm nhiệm vụ trùng lặp:");
        manager.addTask("Mua sách", "Sách Công nghệ phần mềm.", "2025-07-20", "Cao");

        System.out.println("\nThêm nhiệm vụ khác:");
        manager.addTask("Tập thể dục", "Tập gym 1 tiếng.", "2025-07-21", "Trung bình");

        System.out.println("\nThêm nhiệm vụ với tiêu đề rỗng:");
        manager.addTask("", "Nhiệm vụ không có tiêu đề.", "2025-07-22", "Thấp");
    }
}
