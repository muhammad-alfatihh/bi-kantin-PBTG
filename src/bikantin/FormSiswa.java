package bikantin;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FormSiswa extends JFrame {
    private final int idUser;
    private final DefaultTableModel mdlMenu = Koneksi.modelTabel("ID", "Nama Menu", "Harga", "Stok");
    private final DefaultTableModel mdlStatus =
            Koneksi.modelTabel("No. Pesanan", "Menu", "Jumlah", "Total", "Status", "Waktu");
    private final JTable tblMenu = new JTable(mdlMenu);
    private final JTable tblStatus = new JTable(mdlStatus);
    private final JSpinner spJumlah = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));

    public FormSiswa(int idUser, String nama) {
        this.idUser = idUser;
        setTitle("Kantin - " + nama);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 500);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pesan Menu", panelPesan());
        tabs.addTab("Status Pesanan", panelStatus());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> { dispose(); new FormLogin().setVisible(true); });
        JPanel bawah = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bawah.add(btnLogout);

        add(tabs, BorderLayout.CENTER);
        add(bawah, BorderLayout.SOUTH);

        muatMenu();
        muatStatus();

        // Tracking otomatis: refresh status tiap 5 detik
        new javax.swing.Timer(5000, e -> {
            int r = tblStatus.getSelectedRow();
            muatStatus();
            if (r >= 0 && r < mdlStatus.getRowCount()) tblStatus.setRowSelectionInterval(r, r);
        }).start();
    }

    private JPanel panelPesan() {
        JButton btnPesan = new JButton("Pesan");
        JButton btnRefresh = new JButton("Refresh Menu");
        JPanel bawah = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bawah.add(new JLabel("Jumlah:"));
        bawah.add(spJumlah);
        bawah.add(btnPesan);
        bawah.add(btnRefresh);

        btnPesan.addActionListener(e -> pesan());
        btnRefresh.addActionListener(e -> muatMenu());

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(new JScrollPane(tblMenu), BorderLayout.CENTER);
        p.add(bawah, BorderLayout.SOUTH);
        return p;
    }

    private JPanel panelStatus() {
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> muatStatus());
        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        atas.add(btnRefresh);
        atas.add(new JLabel("(otomatis diperbarui tiap 5 detik)"));

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(atas, BorderLayout.NORTH);
        p.add(new JScrollPane(tblStatus), BorderLayout.CENTER);
        return p;
    }

    private void pesan() {
        int r = tblMenu.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih menu dulu."); return; }

        int idMenu = Integer.parseInt(String.valueOf(mdlMenu.getValueAt(r, 0)));
        int harga = Integer.parseInt(String.valueOf(mdlMenu.getValueAt(r, 2)));
        int jumlah = (Integer) spJumlah.getValue();

        try (Connection c = Koneksi.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Kurangi stok hanya jika masih cukup
                PreparedStatement upd = c.prepareStatement(
                        "UPDATE menu SET stok = stok - ? WHERE id = ? AND stok >= ?");
                upd.setInt(1, jumlah); upd.setInt(2, idMenu); upd.setInt(3, jumlah);
                if (upd.executeUpdate() == 0) {
                    c.rollback();
                    JOptionPane.showMessageDialog(this, "Stok tidak mencukupi.");
                    muatMenu();
                    return;
                }
                PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO pesanan(id_user,id_menu,jumlah,total) VALUES(?,?,?,?)");
                ins.setInt(1, idUser); ins.setInt(2, idMenu);
                ins.setInt(3, jumlah); ins.setInt(4, harga * jumlah);
                ins.executeUpdate();
                c.commit();
                JOptionPane.showMessageDialog(this, "Pesanan berhasil! Total: Rp " + (harga * jumlah));
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Gagal memesan: " + ex.getMessage());
        }
        muatMenu();
        muatStatus();
    }

    private void muatMenu() {
        Koneksi.isiTabel(mdlMenu,
                "SELECT id, nama_menu, harga, stok FROM menu WHERE stok > 0 ORDER BY nama_menu");
    }

    private void muatStatus() {
        Koneksi.isiTabel(mdlStatus,
                "SELECT p.id, m.nama_menu, p.jumlah, p.total, p.status, p.waktu "
              + "FROM pesanan p JOIN menu m ON p.id_menu=m.id "
              + "WHERE p.id_user=? ORDER BY p.id DESC", idUser);
    }
}
