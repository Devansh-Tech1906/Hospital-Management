import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HospitalManagementSystem extends JFrame {

    private Connection conn;

    // Tabs
    private JTabbedPane tabbedPane;

    // Patients components
    private JTextField tfPatientName, tfPatientAge, tfPatientGender, tfPatientContact;
    private JButton btnAddPatient, btnViewPatients, btnDeletePatient;
    private JTable tablePatients;
    private DefaultTableModel modelPatients;

    // Doctors components
    private JTextField tfDoctorName, tfDoctorSpec, tfDoctorContact;
    private JButton btnAddDoctor, btnViewDoctors, btnDeleteDoctor;
    private JTable tableDoctors;
    private DefaultTableModel modelDoctors;

    // Appointments components
    private JComboBox<String> cbPatients, cbDoctors;
    private JTextField tfAppointmentDate, tfAppointmentRemarks;
    private JButton btnAddAppointment, btnViewAppointments, btnDeleteAppointment;
    private JTable tableAppointments;
    private DefaultTableModel modelAppointments;

    public HospitalManagementSystem() {
        // Connect to DB
        connectDB();

        // Initialize UI
        initUI();
    }

    private void connectDB() {
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/hospital_db",
                    "postgres",
                    "1234"  // Change this to your actual password
            );
            System.out.println("Connected to DB successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initUI() {
        setTitle("Hospital Management System");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // Add tabs
        tabbedPane.add("Patients", createPatientsPanel());
        tabbedPane.add("Doctors", createDoctorsPanel());
        tabbedPane.add("Appointments", createAppointmentsPanel());

        add(tabbedPane);
    }

    // ===================== PATIENTS TAB =====================
    private JPanel createPatientsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Input fields panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("Name:"));
        tfPatientName = new JTextField();
        inputPanel.add(tfPatientName);

        inputPanel.add(new JLabel("Age:"));
        tfPatientAge = new JTextField();
        inputPanel.add(tfPatientAge);

        inputPanel.add(new JLabel("Gender:"));
        tfPatientGender = new JTextField();
        inputPanel.add(tfPatientGender);

        inputPanel.add(new JLabel("Contact:"));
        tfPatientContact = new JTextField();
        inputPanel.add(tfPatientContact);

        btnAddPatient = new JButton("Add Patient");
        btnViewPatients = new JButton("View Patients");
        btnDeletePatient = new JButton("Delete Selected");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(btnAddPatient);
        buttonsPanel.add(btnViewPatients);
        buttonsPanel.add(btnDeletePatient);

        // Table setup
        modelPatients = new DefaultTableModel(new String[]{"ID", "Name", "Age", "Gender", "Contact"}, 0);
        tablePatients = new JTable(modelPatients);
        JScrollPane scrollPane = new JScrollPane(tablePatients);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        // Button actions
        btnAddPatient.addActionListener(e -> addPatient());
        btnViewPatients.addActionListener(e -> loadPatients());
        btnDeletePatient.addActionListener(e -> deletePatient());

        return panel;
    }

    private void addPatient() {
        String name = tfPatientName.getText();
        String ageText = tfPatientAge.getText();
        String gender = tfPatientGender.getText();
        String contact = tfPatientContact.getText();

        if (name.isEmpty() || ageText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Age are required");
            return;
        }

        try {
            int age = Integer.parseInt(ageText);
            String sql = "INSERT INTO patients(name, age, gender, contact) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, name);
            pst.setInt(2, age);
            pst.setString(3, gender);
            pst.setString(4, contact);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Patient added!");
            clearPatientFields();
            loadPatients();
            loadPatientsIntoComboBox(); // For appointments tab
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Age must be a number");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding patient: " + e.getMessage());
        }
    }

    private void loadPatients() {
        modelPatients.setRowCount(0);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM patients ORDER BY patient_id");
            while (rs.next()) {
                modelPatients.addRow(new Object[]{
                        rs.getInt("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        rs.getString("contact")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading patients: " + e.getMessage());
        }
    }

    private void deletePatient() {
        int selectedRow = tablePatients.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a patient to delete");
            return;
        }
        int id = (int) modelPatients.getValueAt(selectedRow, 0);

        try {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM patients WHERE patient_id = ?");
            pst.setInt(1, id);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Patient deleted!");
            loadPatients();
            loadPatientsIntoComboBox();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting patient: " + e.getMessage());
        }
    }

    private void clearPatientFields() {
        tfPatientName.setText("");
        tfPatientAge.setText("");
        tfPatientGender.setText("");
        tfPatientContact.setText("");
    }

    // ===================== DOCTORS TAB =====================
    private JPanel createDoctorsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("Name:"));
        tfDoctorName = new JTextField();
        inputPanel.add(tfDoctorName);

        inputPanel.add(new JLabel("Specialization:"));
        tfDoctorSpec = new JTextField();
        inputPanel.add(tfDoctorSpec);

        inputPanel.add(new JLabel("Contact:"));
        tfDoctorContact = new JTextField();
        inputPanel.add(tfDoctorContact);

        btnAddDoctor = new JButton("Add Doctor");
        btnViewDoctors = new JButton("View Doctors");
        btnDeleteDoctor = new JButton("Delete Selected");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(btnAddDoctor);
        buttonsPanel.add(btnViewDoctors);
        buttonsPanel.add(btnDeleteDoctor);

        modelDoctors = new DefaultTableModel(new String[]{"ID", "Name", "Specialization", "Contact"}, 0);
        tableDoctors = new JTable(modelDoctors);
        JScrollPane scrollPane = new JScrollPane(tableDoctors);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        btnAddDoctor.addActionListener(e -> addDoctor());
        btnViewDoctors.addActionListener(e -> loadDoctors());
        btnDeleteDoctor.addActionListener(e -> deleteDoctor());

        return panel;
    }

    private void addDoctor() {
        String name = tfDoctorName.getText();
        String spec = tfDoctorSpec.getText();
        String contact = tfDoctorContact.getText();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required");
            return;
        }

        try {
            String sql = "INSERT INTO doctors(name, specialization, contact) VALUES (?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, name);
            pst.setString(2, spec);
            pst.setString(3, contact);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Doctor added!");
            clearDoctorFields();
            loadDoctors();
            loadDoctorsIntoComboBox();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding doctor: " + e.getMessage());
        }
    }

    private void loadDoctors() {
        modelDoctors.setRowCount(0);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM doctors ORDER BY doctor_id");
            while (rs.next()) {
                modelDoctors.addRow(new Object[]{
                        rs.getInt("doctor_id"),
                        rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getString("contact")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading doctors: " + e.getMessage());
        }
    }

    private void deleteDoctor() {
        int selectedRow = tableDoctors.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a doctor to delete");
            return;
        }
        int id = (int) modelDoctors.getValueAt(selectedRow, 0);

        try {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM doctors WHERE doctor_id = ?");
            pst.setInt(1, id);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Doctor deleted!");
            loadDoctors();
            loadDoctorsIntoComboBox();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting doctor: " + e.getMessage());
        }
    }

    private void clearDoctorFields() {
        tfDoctorName.setText("");
        tfDoctorSpec.setText("");
        tfDoctorContact.setText("");
    }

    // ===================== APPOINTMENTS TAB =====================
    private JPanel createAppointmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("Patient:"));
        cbPatients = new JComboBox<>();
        inputPanel.add(cbPatients);

        inputPanel.add(new JLabel("Doctor:"));
        cbDoctors = new JComboBox<>();
        inputPanel.add(cbDoctors);

        inputPanel.add(new JLabel("Date & Time (yyyy-MM-dd HH:mm):"));
        tfAppointmentDate = new JTextField();
        inputPanel.add(tfAppointmentDate);

        inputPanel.add(new JLabel("Remarks:"));
        tfAppointmentRemarks = new JTextField();
        inputPanel.add(tfAppointmentRemarks);

        btnAddAppointment = new JButton("Add Appointment");
        btnViewAppointments = new JButton("View Appointments");
        btnDeleteAppointment = new JButton("Delete Selected");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(btnAddAppointment);
        buttonsPanel.add(btnViewAppointments);
        buttonsPanel.add(btnDeleteAppointment);

        modelAppointments = new DefaultTableModel(new String[]{"ID", "Patient", "Doctor", "Date & Time", "Remarks"}, 0);
        tableAppointments = new JTable(modelAppointments);
        JScrollPane scrollPane = new JScrollPane(tableAppointments);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        btnAddAppointment.addActionListener(e -> addAppointment());
        btnViewAppointments.addActionListener(e -> loadAppointments());
        btnDeleteAppointment.addActionListener(e -> deleteAppointment());

        loadPatientsIntoComboBox();
        loadDoctorsIntoComboBox();

        return panel;
    }

    private void loadPatientsIntoComboBox() {
        cbPatients.removeAllItems();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT patient_id, name FROM patients ORDER BY patient_id");
            while (rs.next()) {
                cbPatients.addItem(rs.getInt("patient_id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading patients for combo box: " + e.getMessage());
        }
    }

    private void loadDoctorsIntoComboBox() {
        cbDoctors.removeAllItems();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT doctor_id, name FROM doctors ORDER BY doctor_id");
            while (rs.next()) {
                cbDoctors.addItem(rs.getInt("doctor_id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading doctors for combo box: " + e.getMessage());
        }
    }

    private void addAppointment() {
        if (cbPatients.getItemCount() == 0 || cbDoctors.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add patients and doctors first.");
            return;
        }

        String patientItem = (String) cbPatients.getSelectedItem();
        String doctorItem = (String) cbDoctors.getSelectedItem();

        if (patientItem == null || doctorItem == null) {
            JOptionPane.showMessageDialog(this, "Please select a patient and doctor.");
            return;
        }

        int patientId = Integer.parseInt(patientItem.split(" - ")[0]);
        int doctorId = Integer.parseInt(doctorItem.split(" - ")[0]);

        String dateStr = tfAppointmentDate.getText();
        String remarks = tfAppointmentRemarks.getText();

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime appointmentDate = LocalDateTime.parse(dateStr, formatter);

            String sql = "INSERT INTO appointments(patient_id, doctor_id, appointment_date, remarks) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, patientId);
            pst.setInt(2, doctorId);
            pst.setTimestamp(3, Timestamp.valueOf(appointmentDate));
            pst.setString(4, remarks);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Appointment added!");
            clearAppointmentFields();
            loadAppointments();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding appointment: " + e.getMessage());
        }
    }

    private void loadAppointments() {
        modelAppointments.setRowCount(0);
        try {
            String sql = "SELECT a.appointment_id, p.name AS patient_name, d.name AS doctor_name, a.appointment_date, a.remarks " +
                    "FROM appointments a " +
                    "JOIN patients p ON a.patient_id = p.patient_id " +
                    "JOIN doctors d ON a.doctor_id = d.doctor_id " +
                    "ORDER BY a.appointment_date";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                modelAppointments.addRow(new Object[]{
                        rs.getInt("appointment_id"),
                        rs.getString("patient_name"),
                        rs.getString("doctor_name"),
                        rs.getTimestamp("appointment_date").toString(),
                        rs.getString("remarks")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading appointments: " + e.getMessage());
        }
    }

    private void deleteAppointment() {
        int selectedRow = tableAppointments.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an appointment to delete");
            return;
        }
        int id = (int) modelAppointments.getValueAt(selectedRow, 0);

        try {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM appointments WHERE appointment_id = ?");
            pst.setInt(1, id);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Appointment deleted!");
            loadAppointments();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting appointment: " + e.getMessage());
        }
    }

    private void clearAppointmentFields() {
        tfAppointmentDate.setText("");
        tfAppointmentRemarks.setText("");
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HospitalManagementSystem().setVisible(true);
        });
    }
}