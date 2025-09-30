package main

import (
	"event_social_platform/config"
	"event_social_platform/internal/handlers"
	"event_social_platform/internal/repository"
	"html/template"
	"log"
	"path/filepath"

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

	sessionRepo := repository.NewSessionRepository(db)
	sessionRepo.CleanExpiredSessions()

	router := gin.Default()

	// ПРАВИЛЬНАЯ настройка шаблонов с наследованием
	router.LoadHTMLGlob("templates/*")

	// Альтернативный способ - загружаем все шаблоны вручную
	// router.SetHTMLTemplate(loadTemplates())

	handlers.SetupRoutes(router, db)

	log.Printf("Server started on http://localhost:%s", cfg.ServerPort)
	if err := router.Run(":" + cfg.ServerPort); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}

// Альтернативная функция для загрузки шаблонов
func loadTemplates() *template.Template {
	templ := template.New("")
	templ.Funcs(template.FuncMap{})

	// Загружаем все HTML файлы из templates
	files, err := filepath.Glob("templates/*.html")
	if err != nil {
		panic(err)
	}

	templ, err = templ.ParseFiles(files...)
	if err != nil {
		panic(err)
	}

	return templ
}
