import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;

public class MovieTicketingSystemtest extends JFrame {
    private JTable movieTable, showTimesTable, bookingsTable;
    private JButton showTimesButton, bookButton, viewBookingsButton;
    private JTextField nameField, cardNumberField, expiryField, cvvField;
    private JComboBox<Integer> seatsCombo;
    private JLabel priceLabel;
    private int selectedMovieId, selectedShowId;
    private double ticketPrice;

    public MovieTicketingSystemtest() {
        setTitle("Movie Ticketing System");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridLayout(3, 1));

        DefaultTableModel movieModel = new DefaultTableModel(new String[]{"Movie ID", "Title", "Genre", "Duration", "Release Date", "Price"}, 0);
        movieTable = new JTable(movieModel);
        fetchMovies(movieModel);

        showTimesButton = new JButton("View Show Times");
        showTimesButton.addActionListener(e -> showShowTimes());
        JPanel moviePanel = new JPanel(new BorderLayout());
        moviePanel.add(new JScrollPane(movieTable), BorderLayout.CENTER);
        moviePanel.add(showTimesButton, BorderLayout.SOUTH);

        DefaultTableModel showTimesModel = new DefaultTableModel(new String[]{"Show ID", "Date", "Time", "Available Seats"}, 0);
        showTimesTable = new JTable(showTimesModel);
        bookButton = new JButton("Book Ticket");
        bookButton.addActionListener(e -> openBookingDialog());
        JPanel showTimesPanel = new JPanel(new BorderLayout());
        showTimesPanel.add(new JScrollPane(showTimesTable), BorderLayout.CENTER);
        showTimesPanel.add(bookButton, BorderLayout.SOUTH);

        DefaultTableModel bookingsModel = new DefaultTableModel(new String[]{"Booking ID", "Customer Name", "Movie", "Show Date", "Show Time", "Seats"}, 0);
        bookingsTable = new JTable(bookingsModel);
        viewBookingsButton = new JButton("View Previous Bookings");
        viewBookingsButton.addActionListener(e -> viewPreviousBookings(bookingsModel));
        JPanel bookingsPanel = new JPanel(new BorderLayout());
        bookingsPanel.add(new JScrollPane(bookingsTable), BorderLayout.CENTER);
        bookingsPanel.add(viewBookingsButton, BorderLayout.SOUTH);

        mainPanel.add(moviePanel);
        mainPanel.add(showTimesPanel);
        mainPanel.add(bookingsPanel);
        add(mainPanel);

        setVisible(true);
    }

    private void fetchMovies(DefaultTableModel model) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM movies")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("movie_id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getInt("duration"),
                        rs.getDate("release_date"),
                        rs.getDouble("price")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showShowTimes() {
        int selectedRow = movieTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedMovieId = (int) movieTable.getValueAt(selectedRow, 0);
            ticketPrice = (double) movieTable.getValueAt(selectedRow, 5);
            DefaultTableModel showTimesModel = (DefaultTableModel) showTimesTable.getModel();
            showTimesModel.setRowCount(0);
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM show_times WHERE movie_id = ?")) {
                stmt.setInt(1, selectedMovieId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    showTimesModel.addRow(new Object[]{rs.getInt("show_id"), rs.getDate("show_date"), rs.getTime("show_time"), rs.getInt("available_seats")});
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a movie.");
        }
    }

    private void openBookingDialog() {
        int selectedRow = showTimesTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedShowId = (int) showTimesTable.getValueAt(selectedRow, 0);
            JDialog bookingDialog = new JDialog(this, "Book Ticket", true);
            bookingDialog.setSize(300, 250);
            bookingDialog.setLayout(new GridLayout(4, 2));
            bookingDialog.setLocationRelativeTo(this);

            bookingDialog.add(new JLabel("Customer Name:"));
            nameField = new JTextField();
            bookingDialog.add(nameField);

            bookingDialog.add(new JLabel("Seats:"));
            seatsCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
            bookingDialog.add(seatsCombo);

            bookingDialog.add(new JLabel("Price:"));
            priceLabel = new JLabel(String.format("Rs%.2f", ticketPrice));
            bookingDialog.add(priceLabel);

            JButton confirmButton = new JButton("Confirm Booking");
            confirmButton.addActionListener(e -> openPaymentDialog(bookingDialog));
            bookingDialog.add(confirmButton);

            bookingDialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a showtime.");
        }
    }

    private void openPaymentDialog(JDialog bookingDialog) {
        JDialog paymentDialog = new JDialog(this, "Payment", true);
        paymentDialog.setSize(350, 300);
        paymentDialog.setLayout(new GridLayout(5, 2));
        paymentDialog.setLocationRelativeTo(this);

        JLabel cardLabel = new JLabel("Card Number:");
        cardNumberField = new JTextField();
        JLabel expiryLabel = new JLabel("Expiry Date (MM/YY):");
        expiryField = new JTextField();
        JLabel cvvLabel = new JLabel("CVV:");
        cvvField = new JTextField();

        JButton payButton = new JButton("Pay");
        payButton.addActionListener(e -> {
            String cardNumber = cardNumberField.getText().trim();
            String expiry = expiryField.getText().trim();
            String cvv = cvvField.getText().trim();

            if (!isValidCardNumber(cardNumber)) {
                JOptionPane.showMessageDialog(this, "Invalid card number. Please enter a 16-digit numeric value.");
                return;
            }

            if (!isValidExpiryDate(expiry)) {
                JOptionPane.showMessageDialog(this, "Invalid expiry date. Please use the MM/YY format and ensure it is a future date.");
                return;
            }

            if (!isValidCVV(cvv)) {
                JOptionPane.showMessageDialog(this, "Invalid CVV. Please enter a 3 or 4-digit numeric value.");
                return;
            }

            completeBooking(paymentDialog, bookingDialog);
        });

        paymentDialog.add(cardLabel);
        paymentDialog.add(cardNumberField);
        paymentDialog.add(expiryLabel);
        paymentDialog.add(expiryField);
        paymentDialog.add(cvvLabel);
        paymentDialog.add(cvvField);
        paymentDialog.add(new JLabel());
        paymentDialog.add(payButton);

        paymentDialog.setVisible(true);
    }

// Helper methods for validation

    private boolean isValidCardNumber(String cardNumber) {
        return cardNumber.matches("\\d{16}");
    }

    private boolean isValidExpiryDate(String expiry) {
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) return false;

        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]) + 2000; // Assume the format is YY

        LocalDate expiryDate = LocalDate.of(year, month, 1).withDayOfMonth(1).plusMonths(1).minusDays(1);
        return expiryDate.isAfter(LocalDate.now());
    }

    private boolean isValidCVV(String cvv) {
        return cvv.matches("\\d{3,4}");
    }


    private void completeBooking(JDialog paymentDialog, JDialog bookingDialog) {
        try (Connection conn = getConnection()) {
            String customerName = nameField.getText();
            int seats = (int) seatsCombo.getSelectedItem();

            String bookingQuery = "INSERT INTO bookings (show_id, customer_name, seats) VALUES (?, ?, ?)";
            PreparedStatement bookingStmt = conn.prepareStatement(bookingQuery);
            bookingStmt.setInt(1, selectedShowId);
            bookingStmt.setString(2, customerName);
            bookingStmt.setInt(3, seats);
            bookingStmt.executeUpdate();

            String updateSeatsQuery = "UPDATE show_times SET available_seats = available_seats - ? WHERE show_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSeatsQuery);
            updateStmt.setInt(1, seats);
            updateStmt.setInt(2, selectedShowId);
            updateStmt.executeUpdate();

            showReceipt(customerName, seats);
            paymentDialog.dispose();
            bookingDialog.dispose();
            showShowTimes();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Booking failed.");
        }
    }

    private void showReceipt(String customerName, int seats) {
        String movieTitle = "";
        String showTimeDetails = "";
        double totalCost = ticketPrice * seats;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT title, show_date, show_time FROM show_times JOIN movies ON show_times.movie_id = movies.movie_id WHERE show_times.show_id = ?")) {
            stmt.setInt(1, selectedShowId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                movieTitle = rs.getString("title");
                Date showDate = rs.getDate("show_date");
                Time showTime = rs.getTime("show_time");
                showTimeDetails = String.format("%s %s", showDate.toString(), showTime.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String receipt = "----- Receipt -----\n" +
                "Customer Name: " + customerName + "\n" +
                "Movie: " + movieTitle + "\n" +
                "Show Time: " + showTimeDetails + "\n" +
                "Seats Booked: " + seats + "\n" +
                "Total Cost: Rs" + totalCost + "\n" +
                "-------------------";

        JOptionPane.showMessageDialog(this, receipt, "Booking Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewPreviousBookings(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT b.booking_id, b.customer_name, m.title, s.show_date, s.show_time, b.seats FROM bookings b JOIN show_times s ON b.show_id = s.show_id JOIN movies m ON s.movie_id = m.movie_id")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("booking_id"), rs.getString("customer_name"), rs.getString("title"), rs.getDate("show_date"), rs.getTime("show_time"), rs.getInt("seats")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/ticketing";
        String user = "root";
        String password = "0000";
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MovieTicketingSystemtest::new);
    }
}