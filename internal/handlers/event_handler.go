package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

type EventHandler struct {
	eventRepo *repository.EventRepository
	userRepo  *repository.UserRepository
}

func NewEventHandler(eventRepo *repository.EventRepository, userRepo *repository.UserRepository) *EventHandler {
	return &EventHandler{
		eventRepo: eventRepo,
		userRepo:  userRepo,
	}
}

func (h *EventHandler) ShowCreateEventForm(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Создание события",
		"NavActive":   "create_event", // ИСПРАВЛЕНО (было "events")
		"CurrentUser": currentUser,
	})
}

func (h *EventHandler) CreateEvent(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.CreateEventRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Создание события",
			"NavActive":   "events",
			"Error":       "Неверные данные формы",
			"CurrentUser": currentUser,
		})
		return
	}

	// Парсим дату и время
	eventTime, err := time.Parse("2006-01-02T15:04", req.DateTime)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Создание события",
			"NavActive":   "events",
			"Error":       "Неверный формат даты и времени",
			"CurrentUser": currentUser,
		})
		return
	}

	event := &models.Event{
		Title:           req.Title,
		Description:     req.Description,
		Type:            req.Type,
		DateTime:        eventTime,
		Location:        req.Location,
		CreatorID:       currentUser.ID,
		IsPrivate:       req.IsPrivate,
		MaxParticipants: req.MaxParticipants,
	}

	if err := h.eventRepo.CreateEvent(event); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Создание события",
			"NavActive":   "events",
			"Error":       "Ошибка при создании события: " + err.Error(),
			"CurrentUser": currentUser,
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/events")
}

func (h *EventHandler) GetAllEvents(c *gin.Context) {
	events, err := h.eventRepo.GetAllEvents()
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "События",
			"NavActive":   "events", // УЖЕ ПРАВИЛЬНО
			"Error":       "Ошибка получения событий",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "События",
		"NavActive":   "events",
		"Events":      events,
		"CurrentUser": currentUser,
	})
}

func (h *EventHandler) GetEvent(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Событие",
			"NavActive":   "events",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Событие",
			"NavActive":   "events",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       event.Title,
		"NavActive":   "events",
		"Event":       event,
		"CurrentUser": currentUser,
	})
}
