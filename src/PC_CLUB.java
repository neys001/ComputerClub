import java.util.Scanner;
import java.sql.*;


public class PC_CLUB {
    private static final String DRIVER = "org.postgresql.Driver";

    private static final String PROTOCOL = "jdbc:postgresql://";
    private static final String URL_LOCALE = "localhost/";
    private static final String DATABASE_NAME = "Computer_Club";
    private static final String DATABASE_URL = PROTOCOL + URL_LOCALE + DATABASE_NAME;

    private static final String USER_NAME = "postgres";
    private static final String DATABASE_PASS = "postgres";


    public static boolean checkDriver () {
        try {
            Class.forName(DRIVER);
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println("Нет JDBC-драйвера! Подключите JDBC-драйвер к проекту согласно инструкции.");
            throw new RuntimeException(e);
        }
    }

    public static boolean checkDB () {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DATABASE_URL, USER_NAME, DATABASE_PASS);
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException ex) {throw new RuntimeException(ex);}
            return true;
        } catch (SQLException e) {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException ex) {throw new RuntimeException(ex);}

            System.out.println("Нет подключения к базе данных! Проверьте имя базы, путь к базе или разверните локально резервную копию согласно инструкции");
            throw new RuntimeException(e);
        }
    }

    public static void showAllClients(Connection connection) throws SQLException {
        try {
            String sql = "SELECT id, name, phone FROM clients ORDER BY id";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                System.out.println("\nСписок всех клиентов:");
                while (rs.next()) {
                    System.out.printf("ID: %d | Имя: %s | Телефон: %s%n",
                            rs.getInt("id"), rs.getString("name"), rs.getString("phone"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    public static void createClient(Connection connection) throws SQLException {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Создание нового клиента");
            System.out.print("Введите имя клиента: ");
            String name = scanner.nextLine();

            System.out.print("Введите номер телефона: ");
            String phone = scanner.nextLine();

            String sql = "INSERT INTO clients (name, phone) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, phone);
                stmt.executeUpdate();
                System.out.println("Клиент успешно добавлен.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка добавления клиента: " + e.getMessage());
        }
    }

    public static void deleteClient(Connection connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        try {
            showAllClients(connection);
            System.out.print("Введите ID клиента для удаления: ");
            int clientId = Integer.parseInt(scanner.nextLine());

            String sql = "DELETE FROM clients WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, clientId);
                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "Клиент удален" : "Клиент не найден!");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    public static void showAllWorkstations(Connection connection) throws SQLException {
        try {
            String sql = """
            SELECT w.id, w.name, z.name as zone_name, z.hourly_rate,
                   CASE WHEN r.id IS NULL THEN 'Свободен' ELSE 'Занят' END as status
            FROM workstations w
            JOIN zones z ON w.zone_id = z.id
            LEFT JOIN reservations r ON w.id = r.workstation_id AND r.status = 'active'
            ORDER BY z.name, w.name
            """;

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                System.out.println("\nСписок всех аппаратов:");
                while (rs.next()) {
                    System.out.printf("ID: %d | %s | Зона: %s | Цена: %.2f | Статус: %s%n",
                            rs.getInt("id"), rs.getString("name"),
                            rs.getString("zone_name"), rs.getDouble("hourly_rate"),
                            rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    public static void addWorkstation(Connection connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("\nДобавление аппарата");

            // Показать доступные зоны
            String zonesSql = "SELECT id, name FROM zones ORDER BY id";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(zonesSql)) {

                System.out.println("Доступные зоны:");
                while (rs.next()) {
                    System.out.printf("ID: %d | %s%n", rs.getInt("id"), rs.getString("name"));
                }
            }

            System.out.print("Введите ID зоны: ");
            int zoneId = Integer.parseInt(scanner.nextLine());

            System.out.print("Введите название аппарата: ");
            String name = scanner.nextLine();

            String sql = "INSERT INTO workstations (zone_id, name) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, zoneId);
                stmt.setString(2, name);
                stmt.executeUpdate();
                System.out.println("Аппарат добавлен.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    public static void deleteWorkstation(Connection connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        try {
            showAllWorkstations(connection);
            System.out.print("Введите ID аппарата для удаления: ");
            int workstationId = Integer.parseInt(scanner.nextLine());

            String sql = "DELETE FROM workstations WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, workstationId);
                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "Аппарат удален!" : "Аппарат не найден!");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }


    public static void createReservation(Connection connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("Создание брони");
            showAvailableWorkstations(connection);

            System.out.print("Введите ID клиента: ");
            int clientId = Integer.parseInt(scanner.nextLine());

            System.out.print("Введите ID рабочего места для брони: ");
            int workstationId = Integer.parseInt(scanner.nextLine());

            String sql = "INSERT INTO reservations (client_id, workstation_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, clientId);
                stmt.setInt(2, workstationId);
                stmt.executeUpdate();
                System.out.println("Рабочее место успешно забронировано.");
            }
        } catch (SQLException e) {
            // Проверяем код ошибки уникального нарушения
            if (e.getSQLState().equals("23505")) {
                System.out.println("Error: Это место уже забронировано.");
            } else {
                System.out.println("Ошибка бронирования: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Введите корректное значение.");
        }
    }

    public static void completeReservation(Connection connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("Отмена бронирования");
            showOccupiedWorkstations(connection);

            System.out.print("Введите ID рабочего места для отмены: ");
            int workstationId = Integer.parseInt(scanner.nextLine());

            String sql = "UPDATE reservations SET status = 'completed' WHERE workstation_id = ? AND status = 'active'";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, workstationId);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    System.out.println("Бронирование отменено.");
                } else {
                    System.out.println("Нет активной брони на это рабочее место.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error ошибка отмены брони: " + e.getMessage());
        }
    }

    public static void showAvailableWorkstations(Connection connection) throws SQLException {

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT w.id, w.name, z.name as zone_name, z.hourly_rate " +
                    "FROM workstations w JOIN zones z ON w.zone_id = z.id " +
                    "LEFT JOIN reservations r ON w.id = r.workstation_id AND r.status = 'active' " +
                    "WHERE r.id IS NULL ORDER BY z.name, w.name");

            System.out.println("\n СВОБОДНЫЕ АППАРАТЫ ");
                while (rs.next()) {
                    System.out.printf("ID: %d | %s | Зона: %s | Цена: %.2f/час%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("zone_name"),
                            rs.getDouble("hourly_rate"));
                }

    }

    public static void showOccupiedWorkstations(Connection connection) throws SQLException {
        try {
            String sql = "SELECT w.id, w.name, z.name as zone_name, c.name as client_name, r.id as reservation_id " +
                    "FROM reservations r " +
                    "JOIN workstations w ON r.workstation_id = w.id " +
                    "JOIN zones z ON w.zone_id = z.id " +
                    "JOIN clients c ON r.client_id = c.id " +
                    "WHERE r.status = 'active' " +
                    "ORDER BY w.id";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                System.out.println("\n ЗАНЯТЫЕ АППАРАТЫ ");
                while (rs.next()) {
                    System.out.printf("ID: %d | %s | Зона: %s | Клиент: %s%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("zone_name"),
                            rs.getString("client_name"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при получении занятых аппаратов: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        if (checkDriver() && checkDB())
            System.out.println("Успешное подключение к базе данных | " + DATABASE_URL + "\n");
        else return;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USER_NAME, DATABASE_PASS)) {

            while (true) {
                System.out.println("\n> Компьютерный клуб <");
                System.out.println("1. Показать всех клиентов");
                System.out.println("2. Добавить клиента");
                System.out.println("3. Удалить клиента");
                System.out.println("4. Показать все аппараты");
                System.out.println("5. Добавить аппарат");
                System.out.println("6. Удалить аппарат");
                System.out.println("7. Показать свободные аппараты");
                System.out.println("8. Показать занятые аппараты");
                System.out.println("9. Создать бронь");
                System.out.println("10. Отменить бронь");
                System.out.println("11. Выход");
                System.out.print("Выберите команду: ");

                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> showAllClients(connection);
                    case "2" -> createClient(connection);
                    case "3" -> deleteClient(connection);
                    case "4" -> showAllWorkstations(connection);
                    case "5" -> addWorkstation(connection);
                    case "6" -> deleteWorkstation(connection);
                    case "7" -> showAvailableWorkstations(connection);
                    case "8" -> showOccupiedWorkstations(connection);
                    case "9" -> createReservation(connection);
                    case "10" -> completeReservation(connection);
                    case "11" -> { System.out.println("Выход!"); return; }
                    default -> System.out.println("Неверная команда!");
                }
            }

        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")){
                System.out.println("Произошло дублирование данных");
            } else throw new RuntimeException(e);
        }
    }
}
