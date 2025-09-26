package handlers

import (
	"event_social_platform/internal/middleware"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

func SetupRoutes(router *gin.Engine, db *gorm.DB) {
	// Инициализируем репозитории
	userRepo := repository.NewUserRepository(db)
	eventRepo := repository.NewEventRepository(db)
	sessionRepo := repository.NewSessionRepository(db)

	// Инициализируем обработчики
	userHandler := NewUserHandler(userRepo)
	eventHandler := NewEventHandler(eventRepo, userRepo)
	authHandler := NewAuthHandler(userRepo, sessionRepo)

	// Middleware аутентификации для ВСЕХ маршрутов
	authMiddleware := middleware.AuthMiddleware(userRepo, sessionRepo)
	router.Use(authMiddleware)

	// Статические файлы
	router.Static("/static", "./static")

	// ==================== ВСЕ МАРШРУТЫ ТРЕБУЮТ АУТЕНТИФИКАЦИИ ====================

	// Главная страница
	router.GET("/", userHandler.ShowHomePage)

	// Профили
	router.GET("/profiles", userHandler.GetAllProfiles)
	router.GET("/profile/:id", userHandler.GetProfile)
	router.GET("/profile", authHandler.ShowProfile) // Мой профиль

	// События
	router.GET("/events", eventHandler.GetAllEvents)
	router.GET("/event/:id", eventHandler.GetEvent)
	router.GET("/create-event", eventHandler.ShowCreateEventForm)
	router.POST("/create-event", eventHandler.CreateEvent)

	// Аутентификация
	router.GET("/login", authHandler.ShowLoginForm)
	router.POST("/login", authHandler.Login)
	router.GET("/logout", authHandler.Logout)

	// Создание профиля
	router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)
}
