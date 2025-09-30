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
	wallRepo := repository.NewWallRepository(db)

	// Инициализируем обработчики
	userHandler := NewUserHandler(userRepo)
	eventHandler := NewEventHandler(eventRepo, userRepo)
	authHandler := NewAuthHandler(userRepo, sessionRepo)
	wallHandler := NewWallHandler(wallRepo, userRepo)

	// Middleware аутентификации (теперь не глобальный)
	authMiddleware := middleware.AuthMiddleware(userRepo, sessionRepo)

	// Статические файлы (доступны без аутентификации)
	router.Static("/static", "./static")

	// ==================== МАРШРУТЫ БЕЗ АУТЕНТИФИКАЦИИ ====================

	// Главная страница
	router.GET("/", userHandler.ShowHomePage)

	// Аутентификация
	router.GET("/login", authHandler.ShowLoginForm)
	router.POST("/login", authHandler.Login)

	// Создание профиля (регистрация)
	router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)

	// ==================== МАРШРУТЫ С АУТЕНТИФИКАЦИЕЙ ====================

	// Группа защищенных маршрутов
	protected := router.Group("/")
	protected.Use(authMiddleware)
	{
		// Профили (стена теперь интегрирована в профиль)
		protected.GET("/profiles", userHandler.GetAllProfiles)
		protected.GET("/profile/:id", userHandler.GetProfile)
		protected.GET("/profile", authHandler.ShowProfile)

		// События
		protected.GET("/events", eventHandler.GetAllEvents)
		protected.GET("/event/:id", eventHandler.GetEvent)
		protected.GET("/create-event", eventHandler.ShowCreateEventForm)
		protected.POST("/create-event", eventHandler.CreateEvent)

		// Действия со стеной
		protected.POST("/wall/post", wallHandler.CreatePost)
		protected.GET("/wall/delete/:id", wallHandler.DeletePost)
		protected.GET("/wall/edit/:id", wallHandler.ShowEditForm)
		protected.POST("/wall/edit/:id", wallHandler.UpdatePost)

		// Выход
		protected.GET("/logout", authHandler.Logout)
	}
}
