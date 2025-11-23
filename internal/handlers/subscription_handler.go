package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

type SubscriptionHandler struct {
	subscriptionRepo *repository.SubscriptionRepository
	userRepo         *repository.UserRepository
	achievementRepo  *repository.AchievementRepository
}

func NewSubscriptionHandler(subscriptionRepo *repository.SubscriptionRepository, userRepo *repository.UserRepository, achievementRepo *repository.AchievementRepository) *SubscriptionHandler {
	return &SubscriptionHandler{
		subscriptionRepo: subscriptionRepo,
		userRepo:         userRepo,
		achievementRepo:  achievementRepo,
	}
}

func (h *SubscriptionHandler) Subscribe(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	followingIDStr := c.Param("id")
	followingID, err := strconv.Atoi(followingIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	// Нельзя подписаться на самого себя
	if currentUser.ID == uint(followingID) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Нельзя подписаться на самого себя",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(followingID))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Пользователь не найден",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем, не подписан ли уже
	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(followingID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка проверки подписки",
			"CurrentUser": currentUser,
		})
		return
	}

	if isSubscribed {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Вы уже подписаны на этого пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	subscription := &models.Subscription{
		FollowerID:  currentUser.ID,
		FollowingID: uint(followingID),
	}

	if err := h.subscriptionRepo.CreateSubscription(subscription); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка создания подписки",
			"CurrentUser": currentUser,
		})
		return
	}
	// Обновляем достижения при подписке на пользователя
	if h.achievementRepo != nil {
		go h.achievementRepo.UpdateAchievementsOnUserSubscribed(currentUser.ID)
	}
	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/profile/" + followingIDStr
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *SubscriptionHandler) Unsubscribe(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	followingIDStr := c.Param("id")
	followingID, err := strconv.Atoi(followingIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем, подписан ли пользователь
	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(followingID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка проверки подписки",
			"CurrentUser": currentUser,
		})
		return
	}

	if !isSubscribed {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Вы не подписаны на этого пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.subscriptionRepo.DeleteSubscription(currentUser.ID, uint(followingID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка отписки",
			"CurrentUser": currentUser,
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/profile/" + followingIDStr
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *SubscriptionHandler) MySubscriptions(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	following, err := h.subscriptionRepo.GetFollowing(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения подписок",
			"CurrentUser": currentUser,
		})
		return
	}

	// Получаем статистику
	followersCount, _ := h.subscriptionRepo.GetFollowersCount(currentUser.ID)
	followingCount, _ := h.subscriptionRepo.GetFollowingCount(currentUser.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "Мои подписки",
		"NavActive":      "subscriptions",
		"User":           currentUser,
		"Following":      following,
		"FollowersCount": followersCount,
		"FollowingCount": followingCount,
		"CurrentUser":    currentUser,
	})
}

func (h *SubscriptionHandler) GetSubscriptionsJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	// Получаем подписки пользователя
	following, err := h.subscriptionRepo.GetFollowing(currentUser.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения подписок",
		})
		return
	}

	// Преобразуем в нужный формат
	var subscriptions []gin.H
	for _, user := range following {
		subscriptions = append(subscriptions, gin.H{
			"id": user.ID,
			"follower": gin.H{
				"id":         currentUser.ID,
				"email":      currentUser.Email,
				"first_name": currentUser.FirstName,
				"last_name":  currentUser.LastName,
				"gender":     currentUser.Gender,
				"age":        currentUser.Age,
				"phone":      currentUser.Phone,
			},
			"following": gin.H{
				"id":         user.ID,
				"email":      user.Email,
				"first_name": user.FirstName,
				"last_name":  user.LastName,
				"gender":     user.Gender,
				"age":        user.Age,
				"phone":      user.Phone,
			},
			"created_at": user.CreatedAt.Format(time.RFC3339),
		})
	}

	c.JSON(http.StatusOK, gin.H{
		"success":      true,
		"subscriptions": subscriptions,
	})
}

// GetSubscriptionStatsJSON - получить статистику подписок пользователя
func (h *SubscriptionHandler) GetSubscriptionStatsJSON(c *gin.Context) {
	userIDStr := c.Param("id")
	userID, err := strconv.Atoi(userIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	followersCount, err := h.subscriptionRepo.GetFollowersCount(uint(userID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения статистики",
		})
		return
	}

	followingCount, err := h.subscriptionRepo.GetFollowingCount(uint(userID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения статистики",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"stats": gin.H{
			"followers_count": followersCount,
			"following_count": followingCount,
		},
	})
}

// SubscribeJSON - подписаться на пользователя (JSON)
func (h *SubscriptionHandler) SubscribeJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	followingIDStr := c.Param("id")
	followingID, err := strconv.Atoi(followingIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	// Нельзя подписаться на самого себя
	if currentUser.ID == uint(followingID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Нельзя подписаться на самого себя",
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(followingID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"success": false,
			"message": "Пользователь не найден",
		})
		return
	}

	// Проверяем, не подписан ли уже
	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(followingID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка проверки подписки",
		})
		return
	}

	if isSubscribed {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Вы уже подписаны на этого пользователя",
		})
		return
	}

	subscription := &models.Subscription{
		FollowerID:  currentUser.ID,
		FollowingID: uint(followingID),
	}

	if err := h.subscriptionRepo.CreateSubscription(subscription); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка создания подписки",
		})
		return
	}

	// Обновляем достижения при подписке на пользователя
	if h.achievementRepo != nil {
		go h.achievementRepo.UpdateAchievementsOnUserSubscribed(currentUser.ID)
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Подписка оформлена",
	})
}

// UnsubscribeJSON - отписаться от пользователя (JSON)
func (h *SubscriptionHandler) UnsubscribeJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	followingIDStr := c.Param("id")
	followingID, err := strconv.Atoi(followingIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	// Проверяем, подписан ли пользователь
	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(followingID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка проверки подписки",
		})
		return
	}

	if !isSubscribed {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Вы не подписаны на этого пользователя",
		})
		return
	}

	if err := h.subscriptionRepo.DeleteSubscription(currentUser.ID, uint(followingID)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка отписки",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Подписка отменена",
	})
}

// CheckSubscriptionJSON - проверить подписку
func (h *SubscriptionHandler) CheckSubscriptionJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	userIDStr := c.Param("id")
	userID, err := strconv.Atoi(userIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(userID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка проверки подписки",
		})
		return
	}

	// Исправленная логика - используем тернарный оператор Go
	message := "Не подписан"
	if isSubscribed {
		message = "Подписка активна"
	}

	c.JSON(http.StatusOK, gin.H{
		"success": isSubscribed,
		"message": message,
	})
}