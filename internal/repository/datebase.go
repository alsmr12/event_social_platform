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
			
			// Добавляем колонку через raw SQL чтобы избежать автоматического создания индекса
			if err := db.Exec("ALTER TABLE events ADD COLUMN invite_code VARCHAR(20)").Error; err != nil {
				return fmt.Errorf("failed to add invite_code column: %w", err)
			}
			log.Println("invite_code column added successfully")
		} else {
			log.Println("invite_code column already exists")
		}
	}

	// Очищаем существующие NULL значения в invite_code
	log.Println("Cleaning up invite_code data...")
	if err := db.Exec("UPDATE events SET invite_code = '' WHERE invite_code IS NULL").Error; err != nil {
		log.Printf("Warning: Could not cleanup invite_code data: %v", err)
	}

	return nil
}