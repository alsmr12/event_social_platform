package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type SubscriptionHandler struct {
	subscriptionRepo *repository.SubscriptionRepository
	userRepo         *repository.UserRepository
}

func NewSubscriptionHandler(subscriptionRepo *repository.SubscriptionRepository, userRepo *repository.UserRepository) *SubscriptionHandler {
	return &SubscriptionHandler{
		subscriptionRepo: subscriptionRepo,
		userRepo:         userRepo,
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
		"NavActive":      "subscriptions", // УЖЕ ПРАВИЛЬНО
		"User":           currentUser,
		"Following":      following,
		"FollowersCount": followersCount,
		"FollowingCount": followingCount,
		"CurrentUser":    currentUser,
	})
}
