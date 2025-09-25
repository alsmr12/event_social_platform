package main

import (
	"event_social_platform/internal/handlers"
	"github.com/gin-gonic/gin"
	"html/template"
	"log"
)

func main() {
	router := gin.Default()

	// Явно загружаем каждый шаблон
	router.SetHTMLTemplate(template.Must(template.ParseGlob("templates/*.html")))

	handlers.SetupRoutes(router)

	println("🚀 Сервер запущен на http://localhost:8080")
	if err := router.Run(":8080"); err != nil {
		log.Fatal("Ошибка запуска сервера:", err)
	}
}
