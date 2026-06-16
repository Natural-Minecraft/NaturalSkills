# 🌿 NaturalSkill

> **Plugin pohon skill berbasis poin untuk server Minecraft Paper/Spigot.**
> Pemain mengumpulkan Skill Points dan membuka cabang kemampuan melalui GUI yang interaktif. Admin dapat mengkonfigurasi kategori, cabang, biaya, prasyarat, dan efek reward secara penuh melalui `config.yml` atau lewat GUI editor in-game.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Paper](https://img.shields.io/badge/Platform-Paper%2FSpigot-blue)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

---

## ✨ Fitur Utama

- 🌳 **Pohon Skill** — Sistem skill tree dengan kategori dan cabang berjenjang (memerlukan prasyarat)
- 🎮 **GUI Interaktif** — Menu inventory 54-slot yang bersih untuk player dan admin
- ⚙️ **Konfigurasi Dinamis** — Tambah/ubah kategori, cabang, ikon, slot GUI, biaya, dan efek dari `config.yml`
- 💰 **Efek Reward Fleksibel** — Mendukung perintah konsol, pesan, bunyi, mata uang Vault, item MMOItems, dan XP mcMMO
- 🔧 **Admin GUI Editor** — Edit biaya skill langsung dari in-game tanpa restart server
- 🔌 **Soft Dependency** — Vault, MMOItems, dan mcMMO bersifat opsional; plugin tetap berfungsi tanpa mereka

---

## 📋 Requirement

| Komponen | Versi Minimum | Keterangan |
|---|---|---|
| Paper / Spigot | 1.21+ | Wajib |
| Java | 21 | Wajib |
| Vault | 1.7.1+ | Opsional (untuk efek ekonomi) |
| MMOItems | Latest | Opsional (untuk reward item kustom) |
| mcMMO | Latest | Opsional (untuk reward XP skill) |

---

## 🚀 Instalasi

1. Download file JAR dari [GitHub Releases](https://github.com/Natural-Minecraft/NaturalSkills/releases/latest)
2. Letakkan file `.jar` ke folder `plugins/` server kamu
3. Restart server (atau gunakan PlugMan untuk hot-load)
4. Edit `plugins/NaturalSkill/config.yml` sesuai kebutuhanmu
5. Edit `plugins/NaturalSkill/messages.yml` untuk menyesuaikan teks pesan
6. Jalankan `/nskill reload` untuk menerapkan perubahan tanpa restart

---

## 📁 Struktur File Konfigurasi

```
plugins/
└── NaturalSkill/
    ├── config.yml          # Konfigurasi utama (kategori, cabang, GUI)
    ├── messages.yml        # Semua pesan yang ditampilkan ke pemain
    └── playerdata/
        └── <UUID>.yml      # Data poin & skill yang terbuka per pemain
```

---

## 🛠️ Konfigurasi (`config.yml`)

### GUI Decorations
```yaml
gui:
  decorations:
    filler:
      material: "GRAY_STAINED_GLASS_PANE"
      name: " "
    back_button:
      material: "ARROW"
      name: "&cBack to Categories"
      slot: 49
```

### Menambah Kategori & Cabang
```yaml
categories:
  nama_kategori:         # ID unik kategori (lowercase, underscore)
    name: "&bNama Tampil"
    icon: "MATERIAL_NAME" # Material Bukkit, contoh: BOOK, IRON_SWORD
    slot: 11              # Slot di menu utama (0-53)
    lore:
      - "&7Deskripsi kategori"
    branches:
      nama_cabang:        # ID unik cabang (lowercase, underscore)
        name: "&eNama Cabang"
        icon: "ENCHANTED_BOOK"
        slot: 20          # Slot di menu kategori (0-53)
        cost: 10          # Biaya dalam Skill Points
        prerequisite: "nama_cabang_lain" # Opsional: ID cabang prasyarat
        lore:
          - "&7Deskripsi cabang."
          - "&7Biaya: &e%cost% Skill Points"
          - "&7Status: %status%"
        effects:
          - "[console] give %player% diamond 5"
          - "[message] &aSelamat! Kamu mendapatkan 5 Diamond!"
          - "[sound] ENTITY_PLAYER_LEVELUP"
```

### Format Efek Reward

| Tipe Efek | Format | Keterangan |
|---|---|---|
| `[console]` | `[console] <perintah>` | Jalankan perintah sebagai konsol |
| `[player]` | `[player] <perintah>` | Jalankan perintah sebagai pemain |
| `[message]` | `[message] <teks>` | Kirim pesan ke pemain |
| `[broadcast]` | `[broadcast] <teks>` | Broadcast ke seluruh server |
| `[sound]` | `[sound] <SOUND_NAME>` | Putar suara ke pemain |
| `[vault]` | `[vault] give <jumlah>` atau `take <jumlah>` | Tambah/kurangi uang Vault |
| `[mmoitems]` | `[mmoitems] <TYPE> <ID> [jumlah]` | Berikan MMOItem ke pemain |
| `[mcmmo]` | `[mcmmo] <skill> <xp>` | Tambahkan XP mcMMO ke skill tertentu |
| `[item]` | `[item] <MATERIAL> <jumlah> [nama]` | Berikan item vanilla ke pemain |

> **Placeholder yang tersedia:** `%player%` (diganti nama pemain)

---

## 💬 Perintah

### Perintah Player
| Perintah | Keterangan |
|---|---|
| `/skill` | Membuka menu GUI pohon skill |
| `/skill help` | Menampilkan informasi bantuan |

### Perintah Admin (`naturalskill.admin`)
| Perintah | Keterangan |
|---|---|
| `/nskill give <pemain> <jumlah>` | Memberikan Skill Points ke pemain |
| `/nskill take <pemain> <jumlah>` | Mengambil Skill Points dari pemain |
| `/nskill set <pemain> <jumlah>` | Mengatur Skill Points pemain ke nilai tertentu |
| `/nskill admin` | Membuka GUI Admin Editor (ubah biaya skill in-game) |
| `/nskill reload` | Muat ulang `config.yml` dan `messages.yml` |
| `/nskill help` | Menampilkan informasi bantuan admin |

---

## 🔑 Permissions

| Node | Default | Keterangan |
|---|---|---|
| `naturalskill.admin` | `op` | Akses ke semua perintah admin `/nskill` |

---

## 🏗️ Build dari Source

> **⚠️ Build dilakukan via GitHub Actions CI/CD, bukan secara lokal.**

```bash
# Clone repositori
git clone https://github.com/Natural-Minecraft/NaturalSkills.git

# Dorong perubahan untuk memicu build otomatis
git add .
git commit -m "feat: deskripsi perubahan"
git push
```

JAR hasil build tersedia di tab [Releases](https://github.com/Natural-Minecraft/NaturalSkills/releases) setelah GitHub Actions selesai.

---

## 👥 Tim Pengembang

| Peran | Username |
|---|---|
| Project Owner | NaturalSMP |
| Developer | MRPS35 |
| Code Reviewer | NaturalSMP Team |

---

## 📄 Lisensi

Plugin ini adalah properti eksklusif **NaturalSMP**. Dilarang mendistribusikan, memodifikasi, atau menggunakan kode ini tanpa izin tertulis dari pemilik.
