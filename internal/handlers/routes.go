package handlers

import (
	"event_social_platform/internal/middleware"
	"event_social_platform/internal/repository"
	"fmt"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"log"
	"strings"
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
	router.GET("/login", authHandler.ShowLoginForm)
	router.POST("/login", authHandler.Login)

	// Создание профиля (регистрация)
	router.GET("/create-profile", userHandler.ShowCreateProfileForm)
	router.POST("/create-profile", userHandler.CreateProfile)

	// Группа защищенных маршрутов
	protected := router.Group("/")
	protected.Use(strictAuth)
	{
		// Добавляем маршрут для карты в группу protected
		protected.GET("/map", func(c *gin.Context) {
			currentUserId := c.GetUint("user_id")
			user, err := userRepo.GetUserByID(currentUserId)
			if err != nil || user == nil {
				c.Redirect(302, "/login")
				return
			}

			// Создаем карту атрибутов для передачи в шаблон
			userAttrs := map[string]interface{}{
				"data-user-id":        user.ID,
				"data-user-city":      user.City,
				"data-user-latitude":  user.Latitude,
				"data-user-longitude": user.Longitude,
			}

			c.HTML(200, "base.html", gin.H{
				"Title":       "Карта событий",
				"NavActive":   "map",
				"CurrentUser": user,
				"UserAttrs":   userAttrs,
			})
		})

		// API для сохранения местоположения пользователя
		protected.POST("/api/user/location", func(c *gin.Context) {
			currentUserId := c.GetUint("user_id")
			var req struct {
				Latitude  float64 `json:"latitude"`
				Longitude float64 `json:"longitude"`
				City      string  `json:"city"`
			}

			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(400, gin.H{"error": "Некорректные данные"})
				return
			}

			// Получаем пользователя
			user, err := userRepo.GetUserByID(currentUserId)
			if err != nil || user == nil {
				c.JSON(404, gin.H{"error": "Пользователь не найден"})
				return
			}

			// Обновляем местоположение
			user.Latitude = req.Latitude
			user.Longitude = req.Longitude
			user.City = req.City

			// Сохраняем изменения
			if err := userRepo.UpdateUser(user); err != nil {
				c.JSON(500, gin.H{"error": "Не удалось обновить местоположение"})
				return
			}

			c.JSON(200, gin.H{"message": "Местоположение успешно обновлено"})
		})

		// Вспомогательная функция для получения приблизительных координат по городу
		getApproximateLocation := func(city string) (float64, float64) {
			// Простой маппинг городов на координаты (в реальном приложении использовать геокодер)
			locations := map[string][2]float64{
				"Москва":          {55.7558, 37.6176},
				"Санкт-Петербург": {59.9343, 30.3351},
				"Новосибирск":     {55.0415, 82.9346},
				"Екатеринбург":    {56.8389, 60.6057},
				"Казань":          {55.7963, 49.1088},
			}
			if coords, exists := locations[city]; exists {
				return coords[0], coords[1]
			}
			return 0, 0 // Неизвестный город
		}

		// Вспомогательная функция для анализа тональности текста
		analyzeSentiment := func(text string) float64 {
			// Простой анализ тональности на основе ключевых слов
			// В реальном приложении использовать NLP модель
			positiveWords := []string{"отлично", "хорошо", "замечательно", "прекрасно", "супер", "круто", "весело", "рад", "счастлив", "люблю", "нравится", "класс", "потрясающе", "восхитительно", "великолепно", "замечательно", "вдохновляет", "восхитительно", "восторг", "удовольствие", "радость"}
			negativeWords := []string{"плохо", "ужасно", "отвратительно", "проблем", "ненавижу", "грустно", "тоскливо", "страшно", "беспокоит", "раздражает", "неприятно", "трагедия", "катастрофа", "кошмар", "печально", "обидно", "разочарован", "вред", "опасно", "недостаток", "ошибка"}

			score := 0.0
			textLower := strings.ToLower(text)

			// Проверяем позитивные слова
			for _, word := range positiveWords {
				if strings.Contains(textLower, word) {
					score += 1.0
				}
			}

			// Проверяем негативные слова
			for _, word := range negativeWords {
				if strings.Contains(textLower, word) {
					score -= 1.0
				}
			}

			// Нормализуем результат в диапазоне -1 до 1
			// Предполагаем, что максимум 10 слов могут повлиять на тональность
			if score > 10 {
				score = 10
			} else if score < -10 {
				score = -10
			}
			return score / 10.0
		}

		// API для получения событий
		router.GET("/api/events", authMiddleware, func(c *gin.Context) {
			currentUserId := c.GetUint("user_id") // Теперь пользователь гарантированно авторизован

			// Фильтр по типу события, если указан
			filterType := c.DefaultQuery("type", "all")
			filter := repository.EventFilter{
				Type: filterType,
			}

			// Получаем все события с фильтрацией
			events, err := eventRepo.GetEventsWithFilter(filter)
			if err != nil {
				c.JSON(500, gin.H{"error": "Failed to get events"})
				return
			}

			// Фильтруем события, к которым пользователь имеет доступ
			var accessibleEvents []gin.H
			for _, event := range events {
				// Всегда включаем публичные события
				if !event.IsPrivate {
					accessibleEvents = append(accessibleEvents, gin.H{
						"id":          event.ID,
						"title":       event.Title,
						"description": event.Description,
						"type":        event.Type,
						"date_time":   event.DateTime,
						"location":    event.Location,
						"latitude":    event.Latitude,
						"longitude":   event.Longitude,
						"creator": gin.H{
							"first_name": event.Creator.FirstName,
							"last_name":  event.Creator.LastName,
						},
					})
					// Для приватных событий проверяем доступ (пользователь авторизован)
				} else {
					hasAccess, _ := eventRepo.CanUserAccessEvent(currentUserId, event.ID)
					if hasAccess {
						accessibleEvents = append(accessibleEvents, gin.H{
							"id":          event.ID,
							"title":       event.Title,
							"description": event.Description,
							"type":        event.Type,
							"date_time":   event.DateTime,
							"location":    event.Location,
							"latitude":    event.Latitude,
							"longitude":   event.Longitude,
							"creator": gin.H{
								"first_name": event.Creator.FirstName,
								"last_name":  event.Creator.LastName,
							},
						})
					}
				}
			}

			c.JSON(200, gin.H{"events": accessibleEvents})
		})

		// API для получения данных для тепловой карты
		router.GET("/api/heatmap", authMiddleware, func(c *gin.Context) {
			currentUserId := c.GetUint("user_id")

			// Получаем все посты пользователей
			posts, err := newsRepo.GetNewsFeed(currentUserId, 100, 0) // Получаем последние 100 постов для пользователя
			if err != nil {
				c.JSON(500, gin.H{"error": "Failed to get posts"})
				return
			}

			// Если нет постов, возвращаем пустую тепловую карту
			if len(posts) == 0 {
				c.JSON(200, gin.H{"points": []interface{}{}})
				return
			}

			// Словарь для хранения данных по локациям
			locationScores := make(map[string]*struct {
				Lat   float64
				Lng   float64
				Score float64
				Count int
			})

			// Анализируем каждый пост
			for _, post := range posts {
				// Пропускаем посты без контента
				if post.Content == "" {
					continue
				}

				// Получаем геолокацию пользователя
				lat, lng := getApproximateLocation(post.Author.City)
				if lat == 0 && lng == 0 {
					continue // Пропускаем, если не можем определить локацию
				}

				// Формируем ключ локации
				key := fmt.Sprintf("%.2f_%.2f", lat, lng)

				// Инициализируем запись, если её нет
				if _, exists := locationScores[key]; !exists {
					locationScores[key] = &struct {
						Lat   float64
						Lng   float64
						Score float64
						Count int
					}{Lat: lat, Lng: lng, Score: 0, Count: 0}
				}

				// Анализ настроения в контенте поста
				score := analyzeSentiment(post.Content)

				// Обновляем накопленные данные
				locationScores[key].Score += score
				locationScores[key].Count++
			}

			// Формируем итоговые данные для тепловой карты
			var heatmapData []map[string]interface{}
			for _, data := range locationScores {
				// Вычисляем средний балл настроения
				avgScore := data.Score / float64(data.Count)
				// Нормализуем значение в диапазоне 0-1
				// -1 (очень негативно) -> 0, 0 (нейтрально) -> 0.5, 1 (очень позитивно) -> 1
				intensity := (avgScore + 1) / 2
				// Ограничиваем диапазон
				if intensity < 0 {
					intensity = 0
				} else if intensity > 1 {
					intensity = 1
				}
				// Добавляем в результат только если есть данные
				if data.Count > 0 {
					heatmapData = append(heatmapData, map[string]interface{}{
						"latitude":  data.Lat,
						"longitude": data.Lng,
						"intensity": intensity,
						"count":     data.Count,
					})
				}
			}

			// Если нет данных для тепловой карты, возвращаем пустой массив
			if len(heatmapData) == 0 {
				c.JSON(200, gin.H{"points": []interface{}{}})
				return
			}

			c.JSON(200, gin.H{"points": heatmapData})
		})

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
}
