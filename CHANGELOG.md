# Changelog — NaturalSkill

Semua perubahan penting pada plugin ini didokumentasikan di sini.
Format berdasarkan [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.1.0] — 2026-06-17

### Added
- **Integrasi PlaceholderAPI & PAPI Expansion**:
  - Ditambahkan placeholder `%nskills_points%` (skill points yang dimiliki).
  - Ditambahkan placeholder `%nskills_unlocked_count%` (jumlah cabang skill yang dibuka).
  - Ditambahkan placeholder `%nskills_iq%`, `%nskills_strength%`, `%nskills_agility%`, `%nskills_psychology%`, `%nskills_communication%` (skor per kategori).
  - Nilai IQ dihitung dari Base IQ 100 + akumulasi poin biaya skill yang telah dibuka pada kategori Intelligence.
- **Sistem Leaderboard & Ranking (Anti-Lag)**:
  - Kelas `LeaderboardManager` untuk memuat data semua pemain secara asinkron dari folder `playerdata` pada saat startup, dan memperbaruinya setiap 5 menit secara berkala.
  - Pembaruan skor leaderboard aktif dilakukan secara instan di memori (real-time) begitu ada pemain yang membuka skill baru.
  - Ditambahkan placeholder rank pemain: `%nskills_<kategori>_top_me%` (mengembalikan angka peringkat pemain tersebut, misal "25", atau "-" jika belum terperingkat).
  - Ditambahkan placeholder leaderboard top: `%nskills_<kategori>_top_<1-10>%` (format `Nama (Skor)`), `%nskills_<kategori>_top_<1-10>_name%` (hanya nama), dan `%nskills_<kategori>_top_<1-10>_value%` (hanya skor).
  
### Changed
- `PlayerData` dan `PlayerManager` sekarang menyimpan nama terakhir pemain online (`name` di file `<uuid>.yml`) untuk mencegah lookup `getOfflinePlayer` yang blocking saat menyusun data leaderboard.
- `paper-plugin.yml` dan `pom.xml` ditambahkan dependensi soft-depend ke `PlaceholderAPI`.

---

## [Unreleased] — 2026-06-16

### Fixed
- **[BUG KRITIS] Parser `[mmoitems]` tidak cocok dengan format konfigurasi default**
  - Format lama di `config.yml`: `[mmoitems] give %player% SWORD EXCALIBUR 1`
  - `EffectEngine` membaca argumen pertama sebagai TYPE dan argumen kedua sebagai ID, sehingga `"give"` terbaca sebagai TYPE dan `"%player%"` terbaca sebagai ID — selalu gagal.
  - **Fix**: Ubah format konfigurasi default menjadi `[mmoitems] SWORD EXCALIBUR 1` agar sesuai dengan parser `EffectEngine`.

- **[BUG KRITIS] Parser `[mcmmo]` tidak cocok dengan format konfigurasi default**
  - Format lama di `config.yml`: `[mcmmo] addxp herbalism 1000`
  - `EffectEngine` membaca argumen pertama sebagai SKILL dan argumen kedua sebagai jumlah XP, sehingga `"addxp"` terbaca sebagai skill name dan `"herbalism"` gagal di-parse sebagai integer — selalu melempar `NumberFormatException`.
  - **Fix**: Ubah format konfigurasi default menjadi `[mcmmo] herbalism 1000` agar sesuai dengan parser `EffectEngine`.

- **[KEBOCORAN MEMORI] Map statis `activeCostEdits` di `SkillGui` tidak dibersihkan saat pemain menutup GUI dengan ESC atau disconnect**
  - Ditambahkan method statis `SkillGui.clearCostEdit(UUID)` untuk memungkinkan cleanup dari luar kelas.
  - Ditambahkan event handler `InventoryCloseEvent` di `GuiListener` yang memanggil `clearCostEdit()` saat inventory plugin ditutup — mencegah akumulasi data UUID di memori.

- **[EKSPLOITASI GUI] `InventoryDragEvent` tidak dicancel pada GUI plugin**
  - Pemain berpotensi memanipulasi item di GUI menggunakan drag.
  - Ditambahkan event handler `InventoryDragEvent` di `GuiListener` yang membatalkan semua aksi drag pada inventory bertipe `SkillInventoryHolder`.

- **[LAG SERVER] `Bukkit.getOfflinePlayer(String)` menyebabkan blocking thread utama**
  - Pemanggilan `Bukkit.getOfflinePlayer(targetName)` dapat melakukan request sinkron ke API Mojang jika profil tidak ada di cache lokal, membekukan server selama beberapa detik.
  - **Fix**: Diganti dengan pendekatan dua-langkah non-blocking:
    1. `Bukkit.getPlayerExact(targetName)` — cek pemain yang sedang online (murah, tidak ada I/O)
    2. `Bukkit.getOfflinePlayerIfCached(targetName)` — lookup hanya dari cache lokal tanpa HTTP request

### Added
- `README.md` — Dokumentasi lengkap plugin: fitur, requirement, instalasi, konfigurasi, tabel format efek, perintah, dan permissions.
- `CHANGELOG.md` — Riwayat perubahan plugin mengikuti format Keep a Changelog.

---

## [1.0.0] — Initial Release (oleh MRPS35)

### Added
- Sistem skill tree berbasis poin dengan 5 kategori default:
  - **Intelligence** (IQ I, IQ II, EQ I)
  - **Strength** (Power I, Durability I)
  - **Agility** (Speed I)
  - **Psychology** (Focus I)
  - **Communication** (Charisma I)
- GUI inventory interaktif 54-slot (Main Menu, Category Menu, Admin Editor Menu)
- `SkillInventoryHolder` untuk membedakan GUI plugin dari chest biasa
- `EffectEngine` dengan dukungan 9 tipe efek reward: `[console]`, `[player]`, `[message]`, `[broadcast]`, `[sound]`, `[vault]`, `[mmoitems]`, `[mcmmo]`, `[item]`
- `HookManager` dengan refleksi dinamis untuk Vault, MMOItems, dan mcMMO (soft dependency)
- `PlayerManager` dengan sistem caching YAML per UUID, auto-load saat join, auto-unload & save saat quit
- `ConfigManager` untuk pengelolaan `config.yml` dan `messages.yml` dengan dukungan defaults dari JAR
- Perintah `/skill` untuk GUI pemain dan `/nskill` untuk admin (give/take/set points, admin GUI, reload)
- Tab completion untuk `/nskill` dengan validasi permission
- Hot-loading support: data semua pemain online dimuat ulang saat plugin diaktifkan (kompatibel dengan PlugMan)
- GUI Admin Cost Editor in-game: ubah biaya skill per branch dengan tombol ±1/±5 SP dan simpan ke `config.yml`
- Sistem prasyarat skill (prerequisite) — cabang hanya dapat dibuka setelah prasyarat terpenuhi
- Sistem enkode warna `&` di semua teks konfigurasi dan pesan
