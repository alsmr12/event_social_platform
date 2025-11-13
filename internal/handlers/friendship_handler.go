package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"
	"log"
	"github.com/gin-gonic/gin"
)

type FriendshipHandler struct {
	friendshipRepo *repository.FriendshipRepository
	userRepo       *repository.UserRepository
}

func NewFriendshipHandler(friendshipRepo *repository.FriendshipRepository, userRepo *repository.UserRepository) *FriendshipHandler {
	return &FriendshipHandler{
		friendshipRepo: friendshipRepo,
		userRepo:       userRepo,
	}
}

func (h *FriendshipHandler) SendFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	// Нельзя добавить в друзья самого себя
	if currentUser.ID == uint(friendID) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Нельзя добавить самого себя в друзья",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(friendID))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Пользователь не найден",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем, не отправили ли уже запрос
	status, err := h.friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(friendID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка проверки статуса дружбы",
			"CurrentUser": currentUser,
		})
		return
	}

	if status != "none" {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Запрос на дружбу уже отправлен или пользователь уже в друзьях",
			"CurrentUser": currentUser,
		})
		return
	}

	friendship := &models.Friendship{
		UserID:   currentUser.ID,
		FriendID: uint(friendID),
		Status:   string(models.FriendshipPending),
	}

	if err := h.friendshipRepo.CreateFriendship(friendship); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка отправки запроса в друзья",
			"CurrentUser": currentUser,
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/profile/" + friendIDStr
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *FriendshipHandler) AcceptFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.friendshipRepo.AcceptFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка принятия запроса в друзья",
			"CurrentUser": currentUser,
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *FriendshipHandler) RejectFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.friendshipRepo.RejectFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка отклонения запроса в друзья",
			"CurrentUser": currentUser,
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *FriendshipHandler) RemoveFriend(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.friendshipRepo.DeleteFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка удаления из друзей",
			"CurrentUser": currentUser,
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *FriendshipHandler) FriendsPage(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем все данные для страницы
	friends, err := h.friendshipRepo.GetFriends(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения списка друзей",
			"CurrentUser": currentUser,
		})
		return
	}

	pendingRequests, err := h.friendshipRepo.GetPendingRequests(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения входящих запросов",
			"CurrentUser": currentUser,
		})
		return
	}

	sentRequests, err := h.friendshipRepo.GetSentRequests(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения исходящих запросов",
			"CurrentUser": currentUser,
		})
		return
	}

	friendsCount, _ := h.friendshipRepo.GetFriendsCount(currentUser.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":           "Мои друзья",
		"NavActive":       "friends", // УЖЕ ПРАВИЛЬНО
		"User":            currentUser,
		"Friends":         friends,
		"PendingRequests": pendingRequests,
		"SentRequests":    sentRequests,
		"FriendsCount":    friendsCount,
		"CurrentUser":     currentUser,
	})
}


// GetFriendsJSON - получить список друзей
func (h *FriendshipHandler) GetFriendsJSON(c *gin.Context) {
    log.Println("GetFriendsJSON called")
    
    currentUser := GetUserFromContext(c)
    if currentUser == nil {
        log.Println("GetFriendsJSON: user not authorized")
        c.JSON(http.StatusUnauthorized, gin.H{
            "success": false,
            "message": "Не авторизован",
        })
        return
    }

    log.Printf("GetFriendsJSON: getting friends for user ID %d", currentUser.ID)
    
    friends, err := h.friendshipRepo.GetFriends(currentUser.ID)
    if err != nil {
        log.Printf("GetFriendsJSON error: %v", err)
        c.JSON(http.StatusInternalServerError, gin.H{
            "success": false,
            "message": "Ошибка получения списка друзей",
        })
        return
    }

    log.Printf("GetFriendsJSON: found %d friends", len(friends))
    
    // Убедимся что возвращаем правильную структуру
    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "friends": friends,
    })
}

// GetPendingRequestsJSON - получить входящие заявки в друзья
func (h *FriendshipHandler) GetPendingRequestsJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	requests, err := h.friendshipRepo.GetPendingRequests(currentUser.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения заявок в друзья",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success":  true,
		"requests": requests,
	})
}

// GetSentRequestsJSON - получить отправленные заявки
func (h *FriendshipHandler) GetSentRequestsJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	requests, err := h.friendshipRepo.GetSentRequests(currentUser.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения отправленных заявок",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success":  true,
		"requests": requests,
	})
}

// SendFriendRequestJSON - отправить заявку в друзья
func (h *FriendshipHandler) SendFriendRequestJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	// Нельзя добавить в друзья самого себя
	if currentUser.ID == uint(friendID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Нельзя добавить самого себя в друзья",
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(friendID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"success": false,
			"message": "Пользователь не найден",
		})
		return
	}

	// Проверяем, не отправили ли уже запрос
	status, err := h.friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(friendID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка проверки статуса дружбы",
		})
		return
	}

	if status != "none" {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Запрос на дружбу уже отправлен или пользователь уже в друзьях",
		})
		return
	}

	friendship := &models.Friendship{
		UserID:   currentUser.ID,
		FriendID: uint(friendID),
		Status:   string(models.FriendshipPending),
	}

	if err := h.friendshipRepo.CreateFriendship(friendship); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка отправки запроса в друзья",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Запрос на дружбу отправлен",
	})
}

// AcceptFriendRequestJSON - принять заявку в друзья
func (h *FriendshipHandler) AcceptFriendRequestJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	if err := h.friendshipRepo.AcceptFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка принятия запроса в друзья",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Запрос в друзья принят",
	})
}

// RejectFriendRequestJSON - отклонить заявку в друзья
func (h *FriendshipHandler) RejectFriendRequestJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	if err := h.friendshipRepo.RejectFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка отклонения запроса в друзья",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Запрос в друзья отклонен",
	})
}

// RemoveFriendJSON - удалить из друзей
func (h *FriendshipHandler) RemoveFriendJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверный ID пользователя",
		})
		return
	}

	if err := h.friendshipRepo.DeleteFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка удаления из друзей",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Пользователь удален из друзей",
	})
}

// GetFriendshipStatusJSON - получить статус дружбы с пользователем
func (h *FriendshipHandler) GetFriendshipStatusJSON(c *gin.Context) {
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

	status, err := h.friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(userID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения статуса дружбы",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"status":  status,
	})
}