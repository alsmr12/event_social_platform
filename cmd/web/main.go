package main

import (
	"event_social_platform/config"
	"event_social_platform/internal/handlers"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"log"
)

func main() {
	// Загружаем конфигурацию
	cfg := config.Load()
	// Подключение к базе данных
	db, err := repository.ConnectDB(&repository.DBConfig{
		Host:     cfg.DBHost,
		Port:     cfg.DBPort,
		User:     cfg.DBUser,
		Password: cfg.DBPassword,
		DBName:   cfg.DBName,
	})
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	err = repository.AutoMigrate(db)
	if err != nil {
		log.Fatal("Failed to migrate database:", err)
	}

	sessionRepo := repository.NewSessionRepository(db)
	sessionRepo.CleanExpiredSessions()

	router := gin.Default()
	router.LoadHTMLGlob("templates/*")
	handlers.SetupRoutes(router, db)

	log.Printf("Server started on http://localhost:%s", cfg.ServerPort)
	if err := router.Run("0.0.0.0:" + cfg.ServerPort); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
