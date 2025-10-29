package handlers

import (
	"event_social_platform/internal/models" // ← ДОБАВЬ ЭТОТ ИМПОРТ
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

type EventSubscriptionHandler struct {
	eventSubRepo *repository.EventSubscriptionRepository
	eventRepo    *repository.EventRepository
}

func NewEventSubscriptionHandler(eventSubRepo *repository.EventSubscriptionRepository, eventRepo *repository.EventRepository) *EventSubscriptionHandler {
	return &EventSubscriptionHandler{
		eventSubRepo: eventSubRepo,
		eventRepo:    eventRepo,
	}
}

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

	// Проверяем, не является ли событие прошедшим
	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.Redirect(http.StatusSeeOther, "/events?message=error")
		return
	}

	// Если событие уже прошло, запрещаем подписку
	if time.Now().After(event.DateTime) {
		c.Redirect(http.StatusSeeOther, "/event/"+eventID+"?message=event_ended")
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

	// Определяем откуда пришел запрос и перенаправляем обратно
	referer := c.Request.Header.Get("Referer")
	if referer != "" && strings.Contains(referer, "/event/") {
		// Если пришли со страницы события - остаемся на ней
		c.Redirect(http.StatusSeeOther, "/event/"+eventID+message)
	} else {
		// Иначе возвращаемся к списку событий
		c.Redirect(http.StatusSeeOther, "/events"+message)
	}
}

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

	// Определяем откуда пришел запрос и перенаправляем обратно
	referer := c.Request.Header.Get("Referer")
	if referer != "" && strings.Contains(referer, "/event/") {
		// Если пришли со страницы события - остаемся на ней
		c.Redirect(http.StatusSeeOther, "/event/"+eventID+message)
	} else {
		// Иначе возвращаемся к списку событий
		c.Redirect(http.StatusSeeOther, "/events"+message)
	}
}

// Получить подписки пользователя
func (h *EventSubscriptionHandler) GetUserSubscriptions(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем параметр фильтра из URL
	filter := c.DefaultQuery("filter", "upcoming")

	var subscriptions []*models.EventSubscription
	var err error

	if filter == "past" {
		subscriptions, err = h.eventSubRepo.GetUserPastSubscriptions(currentUser.ID)
	} else {
		subscriptions, err = h.eventSubRepo.GetUserUpcomingSubscriptions(currentUser.ID)
	}

	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Мои подписки",
			"NavActive":   "event_subscriptions",
			"Error":       "Ошибка получения подписок",
			"CurrentUser": currentUser,
		})
		return
	}

	// Добавляем информацию о подписчиках и статусе события
	for _, subscription := range subscriptions {
		subscribersCount, _ := h.eventSubRepo.GetSubscribersCount(subscription.EventID)
		subscription.Event.SubscribersCount = subscribersCount
		subscription.Event.IsPast = time.Now().After(subscription.Event.DateTime)
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "Мои подписки на события",
		"NavActive":      "event_subscriptions",
		"Subscriptions":  subscriptions,
		"CurrentUser":    currentUser,
		"ShowPastEvents": filter == "past",
	})
}
