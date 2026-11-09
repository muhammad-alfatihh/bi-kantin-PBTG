package bikantin;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class FormLogin extends JFrame {
    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);

    public FormLogin() {
        setTitle("Login Kantin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel judul = new JLabel("KANTIN SEKOLAH", SwingConstants.CENTER);
        judul.setFont(new Font("SansSerif", Font.BOLD, 18));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Username"));
        form.add(txtUser);
        form.add(new JLabel("Password"));
        form.add(txtPass);

        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> login());
        txtPass.addActionListener(e -> login()); // tekan Enter

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        root.add(judul, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(btnLogin, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void login() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan password wajib diisi.");
            return;
        }
        String sql = "SELECT id, nama, role FROM users WHERE username=? AND password=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String nama = rs.getString("nama");
                    String role = rs.getString("role");
                    dispose();
                    if (role.equals("admin")) new FormAdmin(nama).setVisible(true);
                    else new FormSiswa(id, nama).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Username atau password salah.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal konek database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FormLogin().setVisible(true));
    }
}
