package repository

import (
	"event_social_platform/internal/models"
	"fmt"
	"log"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

type DBConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
}

func NewDBConfig(host, port, user, password, dbName string) *DBConfig {
	return &DBConfig{
		Host:     host,
		Port:     port,
		User:     user,
		Password: password,
		DBName:   dbName,
	}
}

func (config *DBConfig) GetDSN() string {
	return fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=disable TimeZone=UTC",
		config.Host, config.User, config.Password, config.DBName, config.Port)
}

func ConnectDB(config *DBConfig) (*gorm.DB, error) {
	db, err := gorm.Open(postgres.Open(config.GetDSN()), &gorm.Config{
		DisableForeignKeyConstraintWhenMigrating: true, // Отключаем автоматические FK
	})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}

	log.Println("Connected to database successfully")
	return db, nil
}

func AutoMigrate(db *gorm.DB) error {
	log.Println("Starting database migration...")

	// Сначала мигрируем все таблицы КРОМЕ events
	tables := []interface{}{
		&models.User{},
		&models.Session{},
		&models.WallPost{},
		&models.Subscription{},
		&models.Friendship{},
		&models.EventSubscription{},
		&models.SocialLink{},
		&models.Achievement{},
		&models.UserAchievement{},
	}

	for _, table := range tables {
		if err := db.AutoMigrate(table); err != nil {
			return fmt.Errorf("failed to migrate table: %w", err)
		}
	}

	log.Println("All tables migrated except events")

	// Теперь обрабатываем events таблицу отдельно
	if err := handleEventsTable(db); err != nil {
		return fmt.Errorf("failed to handle events table: %w", err)
	}

	log.Println("Database migration completed successfully")
	return nil
}

func handleEventsTable(db *gorm.DB) error {
	// Проверяем существование таблицы events
	if !db.Migrator().HasTable(&models.Event{}) {
		log.Println("Creating events table...")
		if err := db.AutoMigrate(&models.Event{}); err != nil {
			return fmt.Errorf("failed to create events table: %w", err)
		}
		log.Println("Events table created successfully")
	} else {
		log.Println("Events table already exists, checking structure...")
		
		// Добавляем колонку invite_code если её нет
		if !db.Migrator().HasColumn(&models.Event{}, "InviteCode") {
			log.Println("Adding invite_code column...")
			if err := db.Exec("ALTER TABLE events ADD COLUMN invite_code VARCHAR(20)").Error; err != nil {
				return fmt.Errorf("failed to add invite_code column: %w", err)
			}
			log.Println("invite_code column added successfully")
		} else {
			log.Println("invite_code column already exists")
		}

		// Добавляем колонку private_key если её нет
		if !db.Migrator().HasColumn(&models.Event{}, "PrivateKey") {
			log.Println("Adding private_key column...")
			if err := db.Exec("ALTER TABLE events ADD COLUMN private_key VARCHAR(12)").Error; err != nil {
				return fmt.Errorf("failed to add private_key column: %w", err)
			}
			log.Println("private_key column added successfully")
		} else {
			log.Println("private_key column already exists")
		}
	}

	// Очищаем существующие NULL значения в invite_code и private_key
	log.Println("Cleaning up invite_code and private_key data...")
	
	// Очищаем invite_code
	if err := db.Exec("UPDATE events SET invite_code = '' WHERE invite_code IS NULL").Error; err != nil {
		log.Printf("Warning: Could not cleanup invite_code data: %v", err)
	}
	
	// Очищаем private_key
	if err := db.Exec("UPDATE events SET private_key = '' WHERE private_key IS NULL").Error; err != nil {
		log.Printf("Warning: Could not cleanup private_key data: %v", err)
	}

	// Тщательно чистим дублирующиеся invite_code
	if err := thoroughlyCleanDuplicateInviteCodes(db); err != nil {
		log.Printf("Warning: Could not fix duplicate invite codes: %v", err)
	}

	// Тщательно чистим дублирующиеся private_key
	if err := thoroughlyCleanDuplicatePrivateKeys(db); err != nil {
		log.Printf("Warning: Could not fix duplicate private keys: %v", err)
	}

	// Создаем индексы если их нет (без условий WHERE для простоты)
	if err := createSimpleEventsIndexes(db); err != nil {
		log.Printf("Warning: Could not create events indexes: %v", err)
	}

	return nil
}

// thoroughlyCleanDuplicateInviteCodes тщательно очищает дублирующиеся invite_code
func thoroughlyCleanDuplicateInviteCodes(db *gorm.DB) error {
	log.Println("Thoroughly cleaning duplicate invite codes...")
	
	// Шаг 1: Находим все дублирующиеся invite_code
	var duplicates []string
	db.Raw(`
		SELECT invite_code FROM events 
		WHERE invite_code IS NOT NULL AND invite_code != ''
		GROUP BY invite_code HAVING COUNT(*) > 1
	`).Scan(&duplicates)

	if len(duplicates) > 0 {
		log.Printf("Found %d duplicate invite codes to clean", len(duplicates))
		
		// Шаг 2: Для каждого дубликата оставляем только первую запись
		for _, code := range duplicates {
			log.Printf("Processing duplicate invite code: %s", code)
			
			// Находим ID первой записи с этим кодом
			var firstID uint
			db.Raw(`
				SELECT id FROM events 
				WHERE invite_code = ? 
				ORDER BY id ASC 
				LIMIT 1
			`, code).Scan(&firstID)
			
			// Очищаем invite_code у всех остальных записей
			result := db.Exec(`
				UPDATE events 
				SET invite_code = '' 
				WHERE invite_code = ? AND id != ?
			`, code, firstID)
			
			if result.Error != nil {
				log.Printf("Error cleaning duplicate %s: %v", code, result.Error)
			} else {
				log.Printf("Cleared invite_code from %d records for code %s", result.RowsAffected, code)
			}
		}
	} else {
		log.Println("No duplicate invite codes found")
	}

	return nil
}

// thoroughlyCleanDuplicatePrivateKeys тщательно очищает дублирующиеся private_key
func thoroughlyCleanDuplicatePrivateKeys(db *gorm.DB) error {
	log.Println("Thoroughly cleaning duplicate private keys...")
	
	// Шаг 1: Находим все дублирующиеся private_key
	var duplicates []string
	db.Raw(`
		SELECT private_key FROM events 
		WHERE private_key IS NOT NULL AND private_key != ''
		GROUP BY private_key HAVING COUNT(*) > 1
	`).Scan(&duplicates)

	if len(duplicates) > 0 {
		log.Printf("Found %d duplicate private keys to clean", len(duplicates))
		
		// Шаг 2: Для каждого дубликата оставляем только первую запись
		for _, key := range duplicates {
			log.Printf("Processing duplicate private key: %s", key)
			
			// Находим ID первой записи с этим ключом
			var firstID uint
			db.Raw(`
				SELECT id FROM events 
				WHERE private_key = ? 
				ORDER BY id ASC 
				LIMIT 1
			`, key).Scan(&firstID)
			
			// Очищаем private_key у всех остальных записей
			result := db.Exec(`
				UPDATE events 
				SET private_key = '' 
				WHERE private_key = ? AND id != ?
			`, key, firstID)
			
			if result.Error != nil {
				log.Printf("Error cleaning duplicate %s: %v", key, result.Error)
			} else {
				log.Printf("Cleared private_key from %d records for key %s", result.RowsAffected, key)
			}
		}
	} else {
		log.Println("No duplicate private keys found")
	}

	return nil
}

// createSimpleEventsIndexes создает простые индексы без сложных условий
func createSimpleEventsIndexes(db *gorm.DB) error {
	// Сначала удаляем существующие индексы если они есть (чтобы избежать конфликтов)
	log.Println("Dropping existing indexes if any...")
	db.Exec("DROP INDEX IF EXISTS idx_events_invite_code")
	db.Exec("DROP INDEX IF EXISTS idx_events_private_key")

	// Создаем индекс для invite_code только если в таблице есть записи с уникальными значениями
	log.Println("Creating invite_code index...")
	
	// Проверяем, есть ли дубликаты после очистки
	var inviteCodeDuplicates int64
	db.Raw(`
		SELECT COUNT(*) FROM (
			SELECT invite_code FROM events 
			WHERE invite_code != '' 
			GROUP BY invite_code HAVING COUNT(*) > 1
		) AS duplicates
	`).Scan(&inviteCodeDuplicates)

	if inviteCodeDuplicates == 0 {
		if err := db.Exec("CREATE UNIQUE INDEX idx_events_invite_code ON events (invite_code)").Error; err != nil {
			log.Printf("Warning: Could not create invite_code index: %v", err)
			log.Println("Creating non-unique index for invite_code instead...")
			db.Exec("CREATE INDEX idx_events_invite_code ON events (invite_code)")
		} else {
			log.Println("Unique invite_code index created successfully")
		}
	} else {
		log.Printf("Cannot create unique index for invite_code - still %d duplicates found", inviteCodeDuplicates)
		log.Println("Creating non-unique index for invite_code instead...")
		db.Exec("CREATE INDEX idx_events_invite_code ON events (invite_code)")
	}

	// Создаем индекс для private_key только если в таблице есть записи с уникальными значениями
	log.Println("Creating private_key index...")
	
	var privateKeyDuplicates int64
	db.Raw(`
		SELECT COUNT(*) FROM (
			SELECT private_key FROM events 
			WHERE private_key != '' 
			GROUP BY private_key HAVING COUNT(*) > 1
		) AS duplicates
	`).Scan(&privateKeyDuplicates)

	if privateKeyDuplicates == 0 {
		if err := db.Exec("CREATE UNIQUE INDEX idx_events_private_key ON events (private_key)").Error; err != nil {
			log.Printf("Warning: Could not create private_key index: %v", err)
			log.Println("Creating non-unique index for private_key instead...")
			db.Exec("CREATE INDEX idx_events_private_key ON events (private_key)")
		} else {
			log.Println("Unique private_key index created successfully")
		}
	} else {
		log.Printf("Cannot create unique index for private_key - still %d duplicates found", privateKeyDuplicates)
		log.Println("Creating non-unique index for private_key instead...")
		db.Exec("CREATE INDEX idx_events_private_key ON events (private_key)")
	}

	return nil
}