package handlers

import (
	"event_social_platform/internal/repository"
	"fmt"
	"github.com/gin-gonic/gin"
	"log"
	"math"
	"strings"
)

// MapHandler структура для обработчиков карты
type MapHandler struct {
	UserRepo        *repository.UserRepository
	EventRepo       *repository.EventRepository
	NewsRepo        *repository.NewsRepository
	EventSubRepo    *repository.EventSubscriptionRepository
	AchievementRepo *repository.AchievementRepository
}

// NewMapHandler создает новый экземпляр MapHandler
func NewMapHandler(userRepo *repository.UserRepository, eventRepo *repository.EventRepository, newsRepo *repository.NewsRepository, eventSubRepo *repository.EventSubscriptionRepository, achievementRepo *repository.AchievementRepository) *MapHandler {
	return &MapHandler{
		UserRepo:        userRepo,
		EventRepo:       eventRepo,
		NewsRepo:        newsRepo,
		EventSubRepo:    eventSubRepo,
		AchievementRepo: achievementRepo,
	}
}

// getApproximateLocation возвращает приблизительные координаты по названию города
func (h *MapHandler) getApproximateLocation(city string) (float64, float64) {
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

// analyzeSentiment анализирует тональность текста
func (h *MapHandler) analyzeSentiment(text string) float64 {
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

// HandleMap отображает страницу карты
func (h *MapHandler) HandleMap(c *gin.Context) {
	userIDInterface, exists := c.Get("user_id")
    if !exists {
        log.Printf("❌ HandleMap: user_id not found in context")
        c.Redirect(302, "/login")
        return
    }

	    currentUserId, ok := userIDInterface.(uint)
    if !ok || currentUserId == 0 {
        log.Printf("❌ HandleMap: invalid user_id: %v (type: %T)", userIDInterface, userIDInterface)
        c.Redirect(302, "/login")
        return
    }
	
	user, err := h.UserRepo.GetUserByID(currentUserId)
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
}

// HandleUserLocation обрабатывает сохранение местоположения пользователя
func (h *MapHandler) HandleUserLocation(c *gin.Context) {
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
	user, err := h.UserRepo.GetUserByID(currentUserId)
	if err != nil || user == nil {
		c.JSON(404, gin.H{"error": "Пользователь не найден"})
		return
	}

	// Обновляем местоположение
	user.Latitude = req.Latitude
	user.Longitude = req.Longitude
	user.City = req.City

	// Сохраняем изменения
	if err := h.UserRepo.UpdateUser(user); err != nil {
		c.JSON(500, gin.H{"error": "Не удалось обновить местоположение"})
		return
	}

	c.JSON(200, gin.H{"message": "Местоположение успешно обновлено"})
}

// HandleHeatmap обрабатывает запрос данных для тепловой карты
func (h *MapHandler) HandleHeatmap(c *gin.Context) {
	currentUserId := c.GetUint("user_id")
	// Проверка на 0 была убрана - middleware уже гарантирует валидность user_id

	log.Printf("Successfully retrieved user_id: %d", currentUserId)

	// Получаем все посты пользователей
	posts, err := h.NewsRepo.GetNewsFeed(currentUserId, 1000, 0) // Увеличили количество постов до 1000
	if err != nil {
		log.Printf("Error getting posts for user %d: %v", currentUserId, err)
		c.JSON(500, gin.H{"error": "Failed to get posts"})
		return
	}

	log.Printf("Loaded %d posts for user %d", len(posts), currentUserId)

	// Словарь для хранения данных по локациям
	locationScores := make(map[string]*struct {
		Lat              float64
		Lng              float64
		Score            float64
		Count            int
		EventCount       int // Количество событий в этой локации
		ParticipantCount int // Общее количество участников
	})

	// Получаем все события и количество участников
	events, err := h.EventRepo.GetAllEvents()
	if err != nil {
		log.Printf("Error getting events: %v", err)
		c.JSON(500, gin.H{"error": "Failed to get events"})
		return
	}
	log.Printf("Loaded %d events", len(events))

	// Создаем мапу для быстрого доступа к количеству участников
	eventParticipants := make(map[uint]int)

	// Для каждого события получаем количество подписчиков напрямую из репозитория
	for _, event := range events {
		count, err := h.EventSubRepo.GetSubscribersCount(event.ID)
		if err != nil {
			// Логируем ошибку, но продолжаем обработку
			log.Printf("Error getting subscriber count for event %d: %v", event.ID, err)
			continue
		}
		eventParticipants[event.ID] = int(count)
	}

	// Анализируем каждый пост
	for _, post := range posts {
		// Пропускаем посты без контента
		if post.Content == "" {
			continue
		}

		// Получаем геолокацию пользователя по координатам профиля
		lat := post.Author.Latitude
		lng := post.Author.Longitude

		// Используем город как fallback, если координаты не заданы
		if lat == 0 && lng == 0 {
			lat, lng = h.getApproximateLocation(post.Author.City)
			if lat == 0 && lng == 0 {
				continue // Пропускаем, если не можем определить локацию
			}
		}

		// Формируем ключ локации
		key := fmt.Sprintf("%.2f_%.2f", lat, lng)

		// Инициализируем запись, если её нет
		if _, exists := locationScores[key]; !exists {
			locationScores[key] = &struct {
				Lat              float64
				Lng              float64
				Score            float64
				Count            int
				EventCount       int
				ParticipantCount int
			}{Lat: lat, Lng: lng, Score: 0, Count: 0, EventCount: 0, ParticipantCount: 0}
		}

		// Анализ настроения в контенте поста
		score := h.analyzeSentiment(post.Content)

		// Обновляем накопленные данные
		locationScores[key].Score += score
		locationScores[key].Count++
	}

	// Добавляем события в тепловую карту
	for _, event := range events {
		// Пропускаем события без координат
		if event.Latitude == 0 || event.Longitude == 0 {
			continue
		}

		// Формируем ключ локации
		key := fmt.Sprintf("%.2f_%.2f", event.Latitude, event.Longitude)

		// Инициализируем запись, если её нет
		if _, exists := locationScores[key]; !exists {
			locationScores[key] = &struct {
				Lat              float64
				Lng              float64
				Score            float64
				Count            int
				EventCount       int
				ParticipantCount int
			}{Lat: event.Latitude, Lng: event.Longitude, Score: 0, Count: 0, EventCount: 0, ParticipantCount: 0}
		}

		// Увеличиваем счетчик событий
		locationScores[key].EventCount++

		// Добавляем количество участников
		if participants, exists := eventParticipants[event.ID]; exists {
			locationScores[key].ParticipantCount += participants
		}

		// Рассчитываем вклад от события и участников
		// Чем больше событий и участников, тем выше позитивный вклад
		// Учитываем как количество событий, так и количество участников
		// Используем логарифмическую шкалу, чтобы избежать перекоса в сторону очень крупных событий
		scoreContribution := 0.2 +
			0.3*math.Log10(float64(locationScores[key].EventCount+1)) +
			0.5*math.Log10(float64(locationScores[key].ParticipantCount+1))

		// Добавляем вклад от события и участников
		locationScores[key].Score += scoreContribution
		// Увеличиваем общий счетчик (для нормализации)
		locationScores[key].Count += 3 // Увеличиваем счетчик, так как это значимое событие
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
				"latitude":         data.Lat,
				"longitude":        data.Lng,
				"intensity":        intensity,
				"count":            data.Count,
				"eventCount":       data.EventCount,
				"participantCount": data.ParticipantCount,
			})
		}
	}

	// Добавляем хотя бы одну точку для тестирования
	if len(heatmapData) == 0 {
		// Добавляем тестовую точку для Москвы
		heatmapData = append(heatmapData, map[string]interface{}{
			"latitude":         55.7558,
			"longitude":        37.6176,
			"intensity":        0.5,
			"count":            1,
			"eventCount":       1,
			"participantCount": 1,
		})
	}

	c.JSON(200, gin.H{
		"points": heatmapData,
		"debug": gin.H{
			"posts_count":           len(posts),
			"events_count":          len(events),
			"location_scores_count": len(locationScores),
			"current_user_id":       currentUserId,
		},
	})
}
