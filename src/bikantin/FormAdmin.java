package bikantin;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FormAdmin extends JFrame {
    private final DefaultTableModel mdlMenu = Koneksi.modelTabel("ID", "Nama Menu", "Harga", "Stok");
    private final DefaultTableModel mdlPesanan =
            Koneksi.modelTabel("ID", "Siswa", "Menu", "Jumlah", "Total", "Status", "Waktu");
    private final JTable tblMenu = new JTable(mdlMenu);
    private final JTable tblPesanan = new JTable(mdlPesanan);

    private final JTextField txtNama = new JTextField(14);
    private final JTextField txtHarga = new JTextField(8);
    private final JTextField txtStok = new JTextField(5);
    private final JComboBox<String> cbStatus =
            new JComboBox<>(new String[]{"Menunggu", "Diproses", "Siap Diambil", "Selesai"});

    public FormAdmin(String namaAdmin) {
        setTitle("Admin Kantin - " + namaAdmin);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Kelola Menu", panelMenu());
        tabs.addTab("Status Pesanan", panelPesanan());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> { dispose(); new FormLogin().setVisible(true); });
        JPanel bawah = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bawah.add(btnLogout);

        add(tabs, BorderLayout.CENTER);
        add(bawah, BorderLayout.SOUTH);

        muatMenu();
        muatPesanan();
    }

    // ---------- Tab Menu ----------
    private JPanel panelMenu() {
        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT));
        input.add(new JLabel("Nama")); input.add(txtNama);
        input.add(new JLabel("Harga")); input.add(txtHarga);
        input.add(new JLabel("Stok")); input.add(txtStok);

        JButton btnTambah = new JButton("Tambah");
        JButton btnUbah = new JButton("Ubah");
        JButton btnHapus = new JButton("Hapus");
        JButton btnBersih = new JButton("Bersihkan");
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tombol.add(btnTambah); tombol.add(btnUbah); tombol.add(btnHapus); tombol.add(btnBersih);

        JPanel atas = new JPanel(new GridLayout(2, 1));
        atas.add(input);
        atas.add(tombol);

        tblMenu.getSelectionModel().addListSelectionListener(e -> {
            int r = tblMenu.getSelectedRow();
            if (r >= 0 && !e.getValueIsAdjusting()) {
                txtNama.setText(String.valueOf(mdlMenu.getValueAt(r, 1)));
                txtHarga.setText(String.valueOf(mdlMenu.getValueAt(r, 2)));
                txtStok.setText(String.valueOf(mdlMenu.getValueAt(r, 3)));
            }
        });

        btnTambah.addActionListener(e -> {
            Integer harga = angka(txtHarga), stok = angka(txtStok);
            if (txtNama.getText().trim().isEmpty() || harga == null || stok == null) return;
            if (Koneksi.jalankan("INSERT INTO menu(nama_menu,harga,stok) VALUES(?,?,?)",
                    txtNama.getText().trim(), harga, stok)) { bersihkan(); muatMenu(); }
        });

        btnUbah.addActionListener(e -> {
            int r = tblMenu.getSelectedRow();
            Integer harga = angka(txtHarga), stok = angka(txtStok);
            if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih menu dulu."); return; }
            if (harga == null || stok == null) return;
            if (Koneksi.jalankan("UPDATE menu SET nama_menu=?, harga=?, stok=? WHERE id=?",
                    txtNama.getText().trim(), harga, stok, mdlMenu.getValueAt(r, 0))) {
                bersihkan(); muatMenu();
            }
        });

        btnHapus.addActionListener(e -> {
            int r = tblMenu.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih menu dulu."); return; }
            if (JOptionPane.showConfirmDialog(this, "Hapus menu ini?", "Konfirmasi",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                // Menu yang sudah pernah dipesan tidak bisa dihapus (FK) -> set stok 0 saja.
                if (Koneksi.jalankan("DELETE FROM menu WHERE id=?", mdlMenu.getValueAt(r, 0))) {
                    bersihkan(); muatMenu();
                }
            }
        });

        btnBersih.addActionListener(e -> bersihkan());

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(atas, BorderLayout.NORTH);
        p.add(new JScrollPane(tblMenu), BorderLayout.CENTER);
        return p;
    }

    // ---------- Tab Pesanan ----------
    private JPanel panelPesanan() {
        JButton btnUpdate = new JButton("Update Status");
        JButton btnRefresh = new JButton("Refresh");
        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        atas.add(new JLabel("Status baru:"));
        atas.add(cbStatus);
        atas.add(btnUpdate);
        atas.add(btnRefresh);

        btnUpdate.addActionListener(e -> {
            int r = tblPesanan.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih pesanan dulu."); return; }
            if (Koneksi.jalankan("UPDATE pesanan SET status=? WHERE id=?",
                    cbStatus.getSelectedItem(), mdlPesanan.getValueAt(r, 0))) muatPesanan();
        });
        btnRefresh.addActionListener(e -> muatPesanan());

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(atas, BorderLayout.NORTH);
        p.add(new JScrollPane(tblPesanan), BorderLayout.CENTER);
        return p;
    }

    private void muatMenu() {
        Koneksi.isiTabel(mdlMenu, "SELECT id, nama_menu, harga, stok FROM menu ORDER BY nama_menu");
    }

    private void muatPesanan() {
        Koneksi.isiTabel(mdlPesanan,
                "SELECT p.id, u.nama, m.nama_menu, p.jumlah, p.total, p.status, p.waktu "
              + "FROM pesanan p JOIN users u ON p.id_user=u.id JOIN menu m ON p.id_menu=m.id "
              + "ORDER BY p.id DESC");
    }

    private void bersihkan() {
        txtNama.setText(""); txtHarga.setText(""); txtStok.setText("");
        tblMenu.clearSelection();
    }

    private Integer angka(JTextField f) {
        try { return Integer.parseInt(f.getText().trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Harga dan stok harus berupa angka.");
            return null;
        }
    }
}
