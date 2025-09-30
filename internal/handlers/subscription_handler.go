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

// Подписаться на пользователя
func (h *SubscriptionHandler) Subscribe(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	followingIDStr := c.Param("id")
	followingID, err := strconv.Atoi(followingIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID пользователя",
		})
		return
	}

	// Нельзя подписаться на самого себя
	if currentUser.ID == uint(followingID) {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Нельзя подписаться на самого себя",
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(followingID))
	if err != nil {
		c.HTML(http.StatusNotFound, "error.html", gin.H{
			"Error": "Пользователь не найден",
		})
		return
	}

	// Проверяем, не подписан ли уже
	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(followingID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка проверки подписки",
		})
		return
	}

	if isSubscribed {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Вы уже подписаны на этого пользователя",
		})
		return
	}

	subscription := &models.Subscription{
		FollowerID:  currentUser.ID,
		FollowingID: uint(followingID),
	}

	if err := h.subscriptionRepo.CreateSubscription(subscription); err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка создания подписки",
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

// Отписаться от пользователя
func (h *SubscriptionHandler) Unsubscribe(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	followingIDStr := c.Param("id")
	followingID, err := strconv.Atoi(followingIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID пользователя",
		})
		return
	}

	// Проверяем, подписан ли пользователь
	isSubscribed, err := h.subscriptionRepo.IsSubscribed(currentUser.ID, uint(followingID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка проверки подписки",
		})
		return
	}

	if !isSubscribed {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Вы не подписаны на этого пользователя",
		})
		return
	}

	if err := h.subscriptionRepo.DeleteSubscription(currentUser.ID, uint(followingID)); err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка отписки",
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

// Страница моих подписок
func (h *SubscriptionHandler) MySubscriptions(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	following, err := h.subscriptionRepo.GetFollowing(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка получения подписок",
		})
		return
	}

	// Получаем статистику
	followersCount, _ := h.subscriptionRepo.GetFollowersCount(currentUser.ID)
	followingCount, _ := h.subscriptionRepo.GetFollowingCount(currentUser.ID)

	c.HTML(http.StatusOK, "subscriptions.html", gin.H{
		"User":           currentUser,
		"Following":      following,
		"FollowersCount": followersCount,
		"FollowingCount": followingCount,
	})
}
