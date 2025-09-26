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
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	c.HTML(http.StatusOK, "create_event.html", gin.H{
		"Title": "Создание события",
	})
}

func (h *EventHandler) CreateEvent(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.CreateEventRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "create_event.html", gin.H{
			"Error": "Неверные данные формы",
		})
		return
	}

	// Парсим дату и время
	eventTime, err := time.Parse("2006-01-02T15:04", req.DateTime)
	if err != nil {
		c.HTML(http.StatusBadRequest, "create_event.html", gin.H{
			"Error": "Неверный формат даты и времени",
		})
		return
	}

	event := &models.Event{
		Title:           req.Title,
		Description:     req.Description,
		Type:            req.Type,
		DateTime:        eventTime,
		Location:        req.Location,
		CreatorID:       user.ID, // Используем ID текущего пользователя
		IsPrivate:       req.IsPrivate,
		MaxParticipants: req.MaxParticipants,
	}

	if err := h.eventRepo.CreateEvent(event); err != nil {
		c.HTML(http.StatusInternalServerError, "create_event.html", gin.H{
			"Error": "Ошибка при создании события: " + err.Error(),
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/events")
}

func (h *EventHandler) GetAllEvents(c *gin.Context) {
	events, err := h.eventRepo.GetAllEvents()
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка получения событий",
		})
		return
	}

	c.HTML(http.StatusOK, "events.html", gin.H{
		"Events": events,
	})
}

func (h *EventHandler) GetEvent(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID события",
		})
		return
	}

	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(http.StatusNotFound, "error.html", gin.H{
			"Error": "Событие не найдено",
		})
		return
	}

	c.HTML(http.StatusOK, "event.html", gin.H{
		"Event": event,
	})
}
