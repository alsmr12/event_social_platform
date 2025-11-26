package handlers

import (
	"event_social_platform/internal/middleware"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"log"
	"net/http"
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
	//router.GET("/login", authHandler.ShowLoginForm)
	//router.POST("/login", authHandler.Login)
	// Аутентификация
	// Web маршруты (только отдают HTML)
	router.GET("/login", func(c *gin.Context) {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
		})
	})

	router.GET("/create-profile", func(c *gin.Context) {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":     "Создание профиля",
			"NavActive": "register",
		})
	})

	// Android / JSON API
	router.POST("/api/login", authHandler.LoginJSON)
	router.POST("/api/register", authHandler.RegisterJSON)
	router.GET("/api/profile", authHandler.ProfileJSON)
	router.POST("/api/profile", userHandler.UpdateProfileJSON)
	router.GET("/api/profiles", userHandler.GetAllProfilesJSON)

	// Создание профиля (регистрация)
	//router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)

	// Группа защищенных маршрутов
	protected := router.Group("/")
	protected.Use(strictAuth)
	{
		// Добавляем маршрут для карты в группу protected
		mapHandler := NewMapHandler(userRepo, eventRepo, newsRepo, eventSubRepo, achievementRepo)
		protected.GET("/map", mapHandler.HandleMap)

		// API для сохранения местоположения пользователя
		protected.POST("/api/user/location", mapHandler.HandleUserLocation)

		// API для получения событий

		// API для получения данных для тепловой карты
		protected.GET("/api/heatmap", mapHandler.HandleHeatmap)

		// Новости
		//protected.GET("/news", newsHandler.ShowNewsFeed)
		protected.GET("/news", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Новости",
				"NavActive":   "news",
				"CurrentUser": GetUserFromContext(c),
			})
		})

		// Профили
		// Используем те же методы, что и для API, аналогично реализации с картой
		//userHandler := NewUserHandler(userRepo)
		protected.GET("/profiles", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Все пользователи",
				"NavActive":   "profiles",
				"CurrentUser": GetUserFromContext(c),
			})
		})
		// В разделе protected добавляем правильные маршруты для профилей:
		/*protected.GET("/profile", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Мой профиль",
				"NavActive":   "my_profile",
				"CurrentUser": GetUserFromContext(c),
			})
		})*/
		protected.GET("/profile", authHandler.ShowProfile)
		protected.GET("/profile/:id", userHandler.GetProfile)

		// В разделе protected добавляем:
		protected.GET("/events", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Все события",
				"NavActive":   "events",
				"CurrentUser": GetUserFromContext(c),
			})
		})

		protected.GET("/create-event", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Создание события",
				"NavActive":   "create_event",
				"CurrentUser": GetUserFromContext(c),
			})
		})

		protected.GET("/social-links", socialHandler.ShowSocialLinksForm)
		protected.POST("/social-links", socialHandler.UpdateSocialLinks)

		// События - общие маршруты
		//protected.GET("/events", eventHandler.GetAllEvents)
		//protected.GET("/create-event", eventHandler.ShowCreateEventForm)
		//protected.POST("/create-event", eventHandler.CreateEvent)

		// В разделе защищенных маршрутов добавь:
		protected.GET("/invite", eventHandler.AccessByInviteCodeForm)

		// И обнови существующий маршрут:
		protected.GET("/invite/:code", eventHandler.AccessByInviteCode)

		/*
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

				// И в API группе добавь:

			}*/
		eventGroup := protected.Group("/event")
		{
			// Web-маршруты (только отдают HTML контейнер)
			eventGroup.GET("/:id", func(c *gin.Context) {
				idStr := c.Param("id")
				c.HTML(http.StatusOK, "base.html", gin.H{
					"Title":       "Событие",
					"NavActive":   "event",
					"EventID":     idStr,
					"CurrentUser": GetUserFromContext(c),
				})
			})

			eventGroup.GET("/private/:key", func(c *gin.Context) {
				privateKey := c.Param("key")
				c.HTML(http.StatusOK, "base.html", gin.H{
					"Title":       "Событие",
					"NavActive":   "event",
					"PrivateKey":  privateKey,
					"CurrentUser": GetUserFromContext(c),
				})
			})

			eventGroup.GET("/edit/:id", func(c *gin.Context) {
				idStr := c.Param("id")
				c.HTML(http.StatusOK, "base.html", gin.H{
					"Title":       "Редактирование события",
					"NavActive":   "edit_event",
					"EventID":     idStr,
					"CurrentUser": GetUserFromContext(c),
				})
			})
			protected.GET("/event-subscriptions", func(c *gin.Context) {
				c.HTML(http.StatusOK, "base.html", gin.H{
					"Title":       "Мои подписки на события",
					"NavActive":   "event_subscriptions",
					"CurrentUser": GetUserFromContext(c),
				})
			})
			// API маршруты (обрабатывают действия)
			eventGroup.POST("/:id/subscribe", eventSubHandler.Subscribe)
			eventGroup.POST("/:id/unsubscribe", eventSubHandler.Unsubscribe)
			//eventGroup.POST("/delete/:id", eventHandler.DeleteEvent)
			//eventGroup.POST("/edit/:id", eventHandler.UpdateEvent)
		}

		// Подписки на события
		//protected.GET("/event-subscriptions", eventSubHandler.GetUserSubscriptions)

		// Действия со стеной
		protected.POST("/wall/post", wallHandler.CreatePost)
		protected.GET("/wall/delete/:id", wallHandler.DeletePost)
		protected.GET("/wall/edit/:id", wallHandler.ShowEditForm)
		protected.POST("/wall/edit/:id", wallHandler.UpdatePost)

		protected.GET("/friends", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Мои друзья",
				"NavActive":   "friends",
				"CurrentUser": GetUserFromContext(c),
			})
		})
		protected.GET("/subscriptions", func(c *gin.Context) {
			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Мои подписки",
				"NavActive":   "subscriptions",
				"CurrentUser": GetUserFromContext(c),
			})
		})
		// Подписки
		//protected.GET("/subscriptions", subscriptionHandler.MySubscriptions)
		protected.GET("/subscribe/:id", subscriptionHandler.Subscribe)
		protected.GET("/unsubscribe/:id", subscriptionHandler.Unsubscribe)

		// Друзья
		//protected.GET("/friends", friendshipHandler.FriendsPage)
		protected.GET("/friends/add/:id", friendshipHandler.SendFriendRequest)
		protected.GET("/friends/accept/:id", friendshipHandler.AcceptFriendRequest)
		protected.GET("/friends/reject/:id", friendshipHandler.RejectFriendRequest)
		protected.GET("/friends/remove/:id", friendshipHandler.RemoveFriend)

		// Награды и рейтинг
		protected.GET("/ratings", achievementHandler.ShowRatings)
		protected.GET("/my-achievements", achievementHandler.ShowMyAchievements)

		protected.GET("/edit-profile", userHandler.ShowEditProfileForm)
		protected.POST("/edit-profile", userHandler.UpdateProfile)
		/*protected.GET("/edit-profile", func(c *gin.Context) {
			currentUser := GetUserFromContext(c)
			if currentUser == nil {
				c.Redirect(http.StatusSeeOther, "/login")
				return
			}

			// Получаем социальные сети пользователя
			socialRepo := repository.NewSocialLinkRepository(db)
			socialLinks, err := socialRepo.GetByUserID(currentUser.ID)
			if err != nil {
				socialLinks = []*models.SocialLink{}
			}

			c.HTML(http.StatusOK, "base.html", gin.H{
				"Title":       "Редактирование профиля",
				"NavActive":   "edit_profile",
				"CurrentUser": currentUser,
				"SocialLinks": socialLinks,
			})
		})*/

		protected.GET("/api/events", eventHandler.GetAllEventsJSON)
		protected.POST("/api/create-event", eventHandler.CreateEventJSON)
		protected.POST("/api/event/:id/subscribe", eventHandler.SubscribeJSON)
		protected.POST("/api/event/:id/unsubscribe", eventHandler.UnsubscribeJSON)

		protected.GET("/api/events/feed", eventHandler.GetEventsFeedJSON)            // Лента событий от подписок
		protected.GET("/api/events/user/:user_id", eventHandler.GetUserEventsJSON)   // События пользователя
		protected.POST("/api/events/join-by-code", eventHandler.JoinEventByCodeJSON) // Присоединиться по коду

		// Выход
		protected.GET("/logout", authHandler.Logout)
	}

	// Инициализируем базовые достижения
	//achievementRepo.InitializeAchievements()
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

		api.PUT("/events/:id", eventHandler.UpdateEventJSON)
		api.DELETE("/events/:id", eventHandler.DeleteEventJSON)
		api.GET("/event/private/:key", eventHandler.GetPrivateEventJSON)
		api.POST("/events/:id/update", eventHandler.UpdateEventJSON)
		api.POST("/events/:id/delete", eventHandler.DeleteEventJSON)
		api.GET("/event/:id", eventHandler.GetEventJSON)
		api.GET("/event-subscriptions", eventSubHandler.GetUserSubscriptionsJSON)
		api.POST("/event-subscriptions/:id/subscribe", eventSubHandler.SubscribeJSON)
		api.POST("/event-subscriptions/:id/unsubscribe", eventSubHandler.UnsubscribeJSON)
		api.GET("/friends/subscriptions", subscriptionHandler.GetSubscriptionsJSON)
		api.GET("/friends/check-subscription/:id", subscriptionHandler.CheckSubscriptionJSON)
		api.POST("/friends/subscribe/:id", subscriptionHandler.SubscribeJSON)
		api.POST("/friends/unsubscribe/:id", subscriptionHandler.UnsubscribeJSON)
		api.GET("/profile/:id/subscription-stats", subscriptionHandler.GetSubscriptionStatsJSON)
		api.GET("/events/filtered", eventHandler.GetAllEventsWithFiltersJSON)
		// Лента новостей
		api.GET("/news", newsHandler.GetNewsFeedJSON)

	}
}
