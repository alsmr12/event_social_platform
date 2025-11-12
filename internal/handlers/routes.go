package handlers

import (
	"event_social_platform/internal/middleware"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"log"
)

func SetupRoutes(router *gin.Engine, db *gorm.DB) {
	// Инициализируем репозитории
	userRepo := repository.NewUserRepository(db)
	eventRepo := repository.NewEventRepository(db)
	sessionRepo := repository.NewSessionRepository(db)
	wallRepo := repository.NewWallRepository(db)
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	friendshipRepo := repository.NewFriendshipRepository(db)
	eventSubRepo := repository.NewEventSubscriptionRepository(db)
	socialRepo := repository.NewSocialLinkRepository(db)
	newsRepo := repository.NewNewsRepository(db)
	achievementRepo := repository.NewAchievementRepository(db)

	// Очистить старые награды
	if err := achievementRepo.ClearAchievements(); err != nil {
		log.Printf("Warning: Could not clear achievements: %v", err)
	} else {
		log.Println("Achievements cleared successfully")
	}

	// Инициализируем достижения
	if err := achievementRepo.InitializeAchievements(); err != nil {
		log.Printf("Warning: Could not initialize achievements: %v", err)
	} else {
		log.Println("Achievements initialized successfully")
	}

	// Обновляем прогресс всех пользователей
	if err := achievementRepo.UpdateAllUsersProgress(); err != nil {
		log.Printf("Warning: Could not update users progress: %v", err)
	} else {
		log.Println("Users progress updated successfully")
	}

	// Инициализируем обработчики
	userHandler := NewUserHandler(userRepo)
	eventHandler := NewEventHandler(eventRepo, userRepo, eventSubRepo)
	authHandler := NewAuthHandler(userRepo, sessionRepo)
	wallHandler := NewWallHandler(wallRepo, userRepo)
	subscriptionHandler := NewSubscriptionHandler(subscriptionRepo, userRepo)
	friendshipHandler := NewFriendshipHandler(friendshipRepo, userRepo)
	eventSubHandler := NewEventSubscriptionHandler(eventSubRepo, eventRepo)
	socialHandler := NewSocialHandler(socialRepo, userRepo)
	newsHandler := NewNewsHandler(newsRepo)
	achievementHandler := NewAchievementHandler(achievementRepo)

	// Middleware аутентификации
	authMiddleware := middleware.AuthMiddleware(userRepo, sessionRepo)
	strictAuth := middleware.StrictAuthMiddleware(userRepo, sessionRepo)

	// Делает доступными файлы из папки static/
	router.Static("/static", "./static")

	// ==================== МАРШРУТЫ БЕЗ АУТЕНТИФИКАЦИИ ====================

	// Главная страница
	router.GET("/", authMiddleware, userHandler.ShowHomePage)

	// Аутентификация
// Веб
router.GET("/login", authHandler.ShowLoginForm)
router.POST("/login", authHandler.Login)

// Android / JSON API
router.POST("/api/login", authHandler.LoginJSON)
router.POST("/api/register", authHandler.RegisterJSON)
router.GET("/api/profile", authHandler.ProfileJSON)
router.GET("/api/profiles", userHandler.GetAllProfilesJSON)

	// Создание профиля (регистрация)
	router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)

	// ==================== МАРШРУТЫ С АУТЕНТИФИКАЦИЕЙ ====================

	// Группа защищенных маршрутов
	protected := router.Group("/")
	protected.Use(strictAuth)
	{
		// Новости
		protected.GET("/news", newsHandler.ShowNewsFeed)

		// Профили
		protected.GET("/profiles", userHandler.GetAllProfiles)
		protected.GET("/profile/:id", userHandler.GetProfile)
		protected.GET("/profile", authHandler.ShowProfile)

		protected.GET("/social-links", socialHandler.ShowSocialLinksForm)
		protected.POST("/social-links", socialHandler.UpdateSocialLinks)

		// События
		protected.GET("/events", eventHandler.GetAllEvents)
		protected.GET("/event/:id", eventHandler.GetEvent)
		protected.GET("/create-event", eventHandler.ShowCreateEventForm)
		protected.POST("/create-event", eventHandler.CreateEvent)
		protected.POST("/event/delete/:id", eventHandler.DeleteEvent)
		protected.GET("/event/edit/:id", eventHandler.ShowEditEventForm)
		protected.POST("/event/edit/:id", eventHandler.UpdateEvent)

		// Действия со стеной
		protected.POST("/wall/post", wallHandler.CreatePost)
		protected.GET("/wall/delete/:id", wallHandler.DeletePost)
		protected.GET("/wall/edit/:id", wallHandler.ShowEditForm)
		protected.POST("/wall/edit/:id", wallHandler.UpdatePost)

		// Подписки
		protected.GET("/subscriptions", subscriptionHandler.MySubscriptions)
		protected.GET("/subscribe/:id", subscriptionHandler.Subscribe)
		protected.GET("/unsubscribe/:id", subscriptionHandler.Unsubscribe)

		// Подписки на события
		protected.POST("/event/:id/subscribe", eventSubHandler.Subscribe)
		protected.POST("/event/:id/unsubscribe", eventSubHandler.Unsubscribe)
		protected.GET("/event-subscriptions", eventSubHandler.GetUserSubscriptions)

		// Друзья
		protected.GET("/friends", friendshipHandler.FriendsPage)
		protected.GET("/friends/add/:id", friendshipHandler.SendFriendRequest)
		protected.GET("/friends/accept/:id", friendshipHandler.AcceptFriendRequest)
		protected.GET("/friends/reject/:id", friendshipHandler.RejectFriendRequest)
		protected.GET("/friends/remove/:id", friendshipHandler.RemoveFriend)

		// Награды и рейтинг ← ДОБАВИТЬ ЭТИ МАРШРУТЫ
		protected.GET("/ratings", achievementHandler.ShowRatings)
		protected.GET("/my-achievements", achievementHandler.ShowMyAchievements)

		protected.GET("/edit-profile", userHandler.ShowEditProfileForm)
		protected.POST("/edit-profile", userHandler.UpdateProfile)

		// Выход
		protected.GET("/logout", authHandler.Logout)
	}

	// Инициализируем базовые достижения
	achievementRepo.InitializeAchievements()
	// ==================== ANDROID API ROUTES ====================
api := router.Group("/api")
api.Use(strictAuth)
{
    // Друзья API
    api.GET("/friends", friendshipHandler.GetFriendsJSON)
    api.GET("/friends/pending", friendshipHandler.GetPendingRequestsJSON)
    api.GET("/friends/sent", friendshipHandler.GetSentRequestsJSON)
    api.GET("/friends/status/:id", friendshipHandler.GetFriendshipStatusJSON)
    api.POST("/friends/add/:id", friendshipHandler.SendFriendRequestJSON)
    api.POST("/friends/accept/:id", friendshipHandler.AcceptFriendRequestJSON)
    api.POST("/friends/reject/:id", friendshipHandler.RejectFriendRequestJSON)
    api.POST("/friends/remove/:id", friendshipHandler.RemoveFriendJSON)
}

}
