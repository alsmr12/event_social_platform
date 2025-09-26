package handlers

import (
	"event_social_platform/internal/repository"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

func SetupRoutes(router *gin.Engine, db *gorm.DB) {
	// Инициализируем репозиторий с БД
	userRepo := repository.NewUserRepository(db)
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
