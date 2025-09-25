package handlers

import (
	"event_social_platform/internal/repository"

	"github.com/gin-gonic/gin" // веб движок для go
)

func SetupRoutes(router *gin.Engine) {
	// Инициализируем репозиторий
	userRepo := repository.NewUserRepository()
	userHandler := NewUserHandler(userRepo)

	// Статические файлы
	router.Static("/static", "./static")

	// Маршруты
	router.GET("/", userHandler.ShowHomePage)
	router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)
	router.GET("/profile/:id", userHandler.GetProfile)
	router.GET("/profiles", userHandler.GetAllProfiles)
}
