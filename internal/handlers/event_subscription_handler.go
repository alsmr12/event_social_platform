package handlers

import (

	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type EventSubscriptionHandler struct {
	eventSubRepo *repository.EventSubscriptionRepository
}

func NewEventSubscriptionHandler(eventSubRepo *repository.EventSubscriptionRepository) *EventSubscriptionHandler {
	return &EventSubscriptionHandler{
		eventSubRepo: eventSubRepo,
	}
}

// Подписаться на событие
// Подписаться на событие
// Подписаться на событие
func (h *EventSubscriptionHandler) Subscribe(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	eventID := c.Param("id")
	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.Redirect(http.StatusSeeOther, "/events")
		return
	}

	// Проверяем, не подписан ли уже
	isSubscribed, err := h.eventSubRepo.IsSubscribed(currentUser.ID, uint(id))
	var message string
	if err != nil {
		message = "?message=error"
	} else if isSubscribed {
		message = "?message=already_subscribed"
	} else {
		err = h.eventSubRepo.Subscribe(currentUser.ID, uint(id))
		if err != nil {
			message = "?message=error"
		} else {
			message = "?message=subscribed"
		}
	}

	c.Redirect(http.StatusSeeOther, "/event/"+eventID+message)
}
// Отписаться от события
// Отписаться от события
func (h *EventSubscriptionHandler) Unsubscribe(c *gin.Context) {
    currentUser := GetUserFromContext(c)
    if currentUser == nil {
        c.Redirect(http.StatusSeeOther, "/login")
        return
    }

    eventID := c.Param("id")
    id, err := strconv.ParseUint(eventID, 10, 32)
    if err != nil {
        c.Redirect(http.StatusSeeOther, "/events")
        return
    }

    // Отписываемся
    err = h.eventSubRepo.Unsubscribe(currentUser.ID, uint(id))
    
    var message string
    if err != nil {
        message = "?message=error"
    } else {
        message = "?message=unsubscribed"
    }

    // Перенаправляем обратно на страницу события с сообщением
    c.Redirect(http.StatusSeeOther, "/event/"+eventID+message)
}
// Получить подписки пользователя
func (h *EventSubscriptionHandler) GetUserSubscriptions(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	subscriptions, err := h.eventSubRepo.GetUserSubscriptions(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Мои подписки",
			"NavActive":   "event_subscriptions",
			"Error":       "Ошибка получения подписок",
			"CurrentUser": currentUser,
		})
		return
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":         "Мои подписки на события",
		"NavActive":     "event_subscriptions",
		"Subscriptions": subscriptions,
		"CurrentUser":   currentUser,
	})
}