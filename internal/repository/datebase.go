package repository

import (
	"event_social_platform/internal/models"
	"fmt"
	"log"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

// DBConfig - конфигурация базы данных
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

// ConnectDB - подключение к базе данных
func ConnectDB(config *DBConfig) (*gorm.DB, error) {
	db, err := gorm.Open(postgres.Open(config.GetDSN()), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}

	log.Println("Connected to database successfully")
	return db, nil
}

// AutoMigrate - автоматическое создание таблиц
func AutoMigrate(db *gorm.DB) error {
	err := db.AutoMigrate(&models.User{})
	if err != nil {
		return fmt.Errorf("failed to auto-migrate database: %w", err)
	}

	log.Println("Database migration completed successfully")
	return nil
}
