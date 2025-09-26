package handlers

import (
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

func SetupRoutes(router *gin.Engine, db *gorm.DB) {
	// Инициализируем репозитории
	userRepo := repository.NewUserRepository(db)
	eventRepo := repository.NewEventRepository(db)

	// Инициализируем обработчики
	userHandler := NewUserHandler(userRepo)
	eventHandler := NewEventHandler(eventRepo, userRepo)

	// Статические файлы
	router.Static("/static", "./static")

	// Маршруты пользователей
	router.GET("/", userHandler.ShowHomePage)
	router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)
	router.GET("/profile/:id", userHandler.GetProfile)
	router.GET("/profiles", userHandler.GetAllProfiles)

	// Маршруты событий
	router.GET("/events", eventHandler.GetAllEvents)
	router.GET("/create-event", eventHandler.ShowCreateEventForm)
	router.POST("/create-event", eventHandler.CreateEvent)
	router.GET("/event/:id", eventHandler.GetEvent)
}
