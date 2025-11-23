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
	eventHandler := NewEventHandler(eventRepo, userRepo, eventSubRepo, achievementRepo)
	authHandler := NewAuthHandler(userRepo, sessionRepo)
	wallHandler := NewWallHandler(wallRepo, userRepo)
	subscriptionHandler := NewSubscriptionHandler(subscriptionRepo, userRepo, achievementRepo)
	friendshipHandler := NewFriendshipHandler(friendshipRepo, userRepo, achievementRepo)
	eventSubHandler := NewEventSubscriptionHandler(eventSubRepo, eventRepo, achievementRepo)
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
router.POST("/api/profile", userHandler.UpdateProfileJSON)
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

		// События - общие маршруты
		protected.GET("/events", eventHandler.GetAllEvents)
		protected.GET("/create-event", eventHandler.ShowCreateEventForm)
		protected.POST("/create-event", eventHandler.CreateEvent)

		// В разделе защищенных маршрутов добавь:
		protected.GET("/invite", eventHandler.AccessByInviteCodeForm)

		// И обнови существующий маршрут:
		protected.GET("/invite/:code", eventHandler.AccessByInviteCode)

		// Маршруты для событий
		eventGroup := protected.Group("/event")
		{
			// Публичные события по ID
			eventGroup.GET("/:id", eventHandler.GetEvent)

			// Приватные события по уникальному ключу
			eventGroup.GET("/private/:key", eventHandler.GetPrivateEvent)

			// Подписки на события
			eventGroup.POST("/:id/subscribe", eventSubHandler.Subscribe)
			eventGroup.POST("/:id/unsubscribe", eventSubHandler.Unsubscribe)

			// Управление событиями (только для создателя)
			eventGroup.POST("/delete/:id", eventHandler.DeleteEvent)
			eventGroup.GET("/edit/:id", eventHandler.ShowEditEventForm)
			eventGroup.POST("/edit/:id", eventHandler.UpdateEvent)
		}

		// Подписки на события
		protected.GET("/event-subscriptions", eventSubHandler.GetUserSubscriptions)

		// Действия со стеной
		protected.POST("/wall/post", wallHandler.CreatePost)
		protected.GET("/wall/delete/:id", wallHandler.DeletePost)
		protected.GET("/wall/edit/:id", wallHandler.ShowEditForm)
		protected.POST("/wall/edit/:id", wallHandler.UpdatePost)

		// Подписки
		protected.GET("/subscriptions", subscriptionHandler.MySubscriptions)
		protected.GET("/subscribe/:id", subscriptionHandler.Subscribe)
		protected.GET("/unsubscribe/:id", subscriptionHandler.Unsubscribe)

		// Друзья
		protected.GET("/friends", friendshipHandler.FriendsPage)
		protected.GET("/friends/add/:id", friendshipHandler.SendFriendRequest)
		protected.GET("/friends/accept/:id", friendshipHandler.AcceptFriendRequest)
		protected.GET("/friends/reject/:id", friendshipHandler.RejectFriendRequest)
		protected.GET("/friends/remove/:id", friendshipHandler.RemoveFriend)

		// Награды и рейтинг
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
api.POST("/friends/cancel/:id", friendshipHandler.CancelFriendRequestJSON)
	api.PUT("/profile", userHandler.UpdateProfileJSON) 
    api.GET("/profile/stats", userHandler.GetUserStatsJSON) 
api.GET("/wall/posts/:user_id", wallHandler.GetUserWallPostsJSON)
api.POST("/wall/posts", wallHandler.CreatePostJSON)
api.PUT("/wall/posts/:id", wallHandler.UpdatePostJSON)
api.DELETE("/wall/posts/:id", wallHandler.DeletePostJSON)
	


api.GET("/friends/subscriptions", subscriptionHandler.GetSubscriptionsJSON)
api.GET("/friends/check-subscription/:id", subscriptionHandler.CheckSubscriptionJSON)
api.POST("/friends/subscribe/:id", subscriptionHandler.SubscribeJSON)
api.POST("/friends/unsubscribe/:id", subscriptionHandler.UnsubscribeJSON)
api.GET("/profile/:id/subscription-stats", subscriptionHandler.GetSubscriptionStatsJSON)

// Лента новостей
api.GET("/news", newsHandler.GetNewsFeedJSON)
}
}