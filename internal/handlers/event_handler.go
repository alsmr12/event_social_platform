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
	eventRepo    *repository.EventRepository
	userRepo     *repository.UserRepository
	eventSubRepo *repository.EventSubscriptionRepository
}

func NewEventHandler(eventRepo *repository.EventRepository, userRepo *repository.UserRepository, eventSubRepo *repository.EventSubscriptionRepository) *EventHandler {
	return &EventHandler{
		eventRepo:    eventRepo,
		userRepo:     userRepo,
		eventSubRepo: eventSubRepo,
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
		"NavActive":   "create_event",
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
			"NavActive":   "event",
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
			"NavActive":   "event",
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
			"NavActive":   "event",
			"Error":       "Ошибка при создании события: " + err.Error(),
			"CurrentUser": currentUser,
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/events")
}

func (h *EventHandler) GetAllEvents(c *gin.Context) {
	// Получаем параметр фильтра из URL
	filter := c.DefaultQuery("filter", "upcoming")

	var events []*models.Event
	var err error

	// Получаем события в зависимости от фильтра
	if filter == "past" {
		events, err = h.eventRepo.GetPastEvents()
	} else {
		events, err = h.eventRepo.GetUpcomingEvents()
	}

	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "События",
			"NavActive":   "events",
			"Error":       "Ошибка получения событий",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)

	// ФИЛЬТРУЕМ: убираем приватные события, если пользователь не создатель
	var filteredEvents []*models.Event
	for _, event := range events {
		if !event.IsPrivate {
			// Публичное событие - показываем всем
			filteredEvents = append(filteredEvents, event)
		} else if currentUser != nil && event.CreatorID == currentUser.ID {
			// Приватное событие, но пользователь - создатель
			filteredEvents = append(filteredEvents, event)
		}
		// Приватное событие другого пользователя - не показываем
	}

	// Для каждого события получаем информацию о подписках и определяем, прошло ли оно
	for _, event := range filteredEvents {
		// Добавляем информацию о подписках
		if currentUser != nil {
			isSubscribed, _ := h.eventSubRepo.IsSubscribed(currentUser.ID, event.ID)
			event.IsSubscribed = isSubscribed
		}

		subscribersCount, _ := h.eventSubRepo.GetSubscribersCount(event.ID)
		event.SubscribersCount = subscribersCount

		// Определяем, является ли событие прошедшим
		event.IsPast = time.Now().After(event.DateTime)
	}

	// Получаем сообщение из URL
	message := c.Query("message")

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "События",
		"NavActive":      "events",
		"Events":         filteredEvents, // ← используем отфильтрованный список
		"CurrentUser":    currentUser,
		"Message":        message,
		"ShowPastEvents": filter == "past",
	})
}

func (h *EventHandler) GetEvent(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Событие",
			"NavActive":   "event",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Событие",
			"NavActive":   "event",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)

	// УБИРАЕМ ПРОВЕРКИ ДОСТУПА - приватные события открываются для всех по ссылке

	// Получаем информацию о подписке
	var isSubscribed bool
	var subscribersCount int64
	if currentUser != nil {
		isSubscribed, _ = h.eventSubRepo.IsSubscribed(currentUser.ID, event.ID)
		subscribersCount, _ = h.eventSubRepo.GetSubscribersCount(event.ID)
	}

	// Определяем, является ли событие прошедшим
	event.IsPast = time.Now().After(event.DateTime)

	message := c.Query("message")
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":            event.Title,
		"NavActive":        "event",
		"Event":            event,
		"CurrentUser":      currentUser,
		"IsSubscribed":     isSubscribed,
		"SubscribersCount": subscribersCount,
		"Message":          message,
	})
}

func (h *EventHandler) DeleteEvent(c *gin.Context) {
	// Получаем текущего пользователя из контекста
	currentUser, exists := c.Get("CurrentUser")
	if !exists {
		c.Redirect(302, "/login")
		return
	}

	user := currentUser.(*models.User)

	// Получаем ID события из URL
	eventID := c.Param("id")

	// Преобразуем в число
	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.JSON(400, gin.H{"error": "Invalid event ID"})
		return
	}

	// Получаем событие из базы данных
	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.JSON(404, gin.H{"error": "Event not found"})
		return
	}

	// Проверяем, является ли пользователь создателем события
	if event.CreatorID != user.ID {
		c.JSON(403, gin.H{"error": "Access denied"})
		return
	}

	// Удаляем событие
	err = h.eventRepo.DeleteEvent(uint(id))
	if err != nil {
		c.JSON(500, gin.H{"error": "Error deleting event"})
		return
	}

	// Перенаправляем обратно на страницу событий
	c.Redirect(302, "/events")
}

// Показать форму редактирования
func (h *EventHandler) ShowEditEventForm(c *gin.Context) {
	eventID := c.Param("id")

	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "events",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(404, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "events",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)
	if currentUser == nil || currentUser.ID != event.CreatorID {
		c.HTML(403, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "events",
			"Error":       "Доступ запрещен",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	c.HTML(200, "base.html", gin.H{
		"Title":       "Редактировать событие",
		"NavActive":   "edit_event",
		"Event":       event,
		"CurrentUser": currentUser,
	})
}

// Обновить событие
func (h *EventHandler) UpdateEvent(c *gin.Context) {
	eventID := c.Param("id")

	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Получаем существующее событие
	existingEvent, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(404, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Проверяем, что текущий пользователь - создатель события
	currentUser := GetUserFromContext(c)
	if currentUser == nil || currentUser.ID != existingEvent.CreatorID {
		c.HTML(403, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Error":       "Доступ запрещен",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Парсим форму
	var form models.CreateEventRequest
	if err := c.ShouldBind(&form); err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Event":       existingEvent,
			"CurrentUser": currentUser,
			"Error":       "Неверные данные формы",
		})
		return
	}

	// Парсим дату и время
	dateTime, err := time.Parse("2006-01-02T15:04", form.DateTime)
	if err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Event":       existingEvent,
			"CurrentUser": currentUser,
			"Error":       "Неверный формат даты и времени",
		})
		return
	}

	// Обновляем поля события
	existingEvent.Title = form.Title
	existingEvent.Description = form.Description
	existingEvent.Type = form.Type
	existingEvent.DateTime = dateTime
	existingEvent.Location = form.Location
	existingEvent.IsPrivate = form.IsPrivate
	existingEvent.MaxParticipants = form.MaxParticipants

	// Обрабатываем координаты
	if form.Latitude != "" {
		if lat, err := strconv.ParseFloat(form.Latitude, 64); err == nil {
			existingEvent.Latitude = lat
		}
	}
	if form.Longitude != "" {
		if lng, err := strconv.ParseFloat(form.Longitude, 64); err == nil {
			existingEvent.Longitude = lng
		}
	}

	// Сохраняем изменения
	err = h.eventRepo.UpdateEvent(existingEvent)
	if err != nil {
		c.HTML(500, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Event":       existingEvent,
			"CurrentUser": currentUser,
			"Error":       "Ошибка при сохранении изменений",
		})
		return
	}

	// Перенаправляем на страницу события
	c.Redirect(302, "/event/"+eventID)
}
