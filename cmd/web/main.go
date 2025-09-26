package main

import (
	"event_social_platform/config"
	"event_social_platform/internal/handlers"
	"event_social_platform/internal/repository"
	"log"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()

	dbConfig := repository.NewDBConfig(
		cfg.DBHost,
		cfg.DBPort,
		cfg.DBUser,
		cfg.DBPassword,
		cfg.DBName,
	)

	db, err := repository.ConnectDB(dbConfig)
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	err = repository.AutoMigrate(db)
	if err != nil {
		log.Fatal("Failed to migrate database:", err)
	}

	// После подключения к БД добавим:
	sessionRepo := repository.NewSessionRepository(db)
	sessionRepo.CleanExpiredSessions()

	router := gin.Default()
	router.LoadHTMLGlob("templates/*")
	handlers.SetupRoutes(router, db)

	log.Printf("Server started on http://localhost:%s", cfg.ServerPort)
	if err := router.Run(":" + cfg.ServerPort); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
