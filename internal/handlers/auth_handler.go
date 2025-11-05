package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type AuthHandler struct {
	userRepo    *repository.UserRepository
	sessionRepo *repository.SessionRepository
}

func NewAuthHandler(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) *AuthHandler {
	return &AuthHandler{
		userRepo:    userRepo,
		sessionRepo: sessionRepo,
	}
}

// ---------- WEB METHODS (старые, для HTML) ----------

func (h *AuthHandler) ShowLoginForm(c *gin.Context) {
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":     "Вход в систему",
		"NavActive": "login",
	})
}

func (h *AuthHandler) Login(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный email или пароль",
		})
		return
	}

	user, err := h.userRepo.GetUserByEmail(req.Email)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный email",
		})
		return
	}

	if !user.CheckPassword(req.Password) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный пароль",
		})
		return
	}

	token := uuid.New().String()
	session := &models.Session{
		UserID:    user.ID,
		Token:     token,
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	if err := h.sessionRepo.CreateSession(session); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Ошибка создания сессии",
		})
		return
	}

	c.SetCookie("session_token", token, 3600*24, "/", "", false, true)
	c.Redirect(http.StatusSeeOther, "/profile")
}

func (h *AuthHandler) Logout(c *gin.Context) {
	token, err := c.Cookie("session_token")
	if err == nil {
		h.sessionRepo.DeleteSession(token)
	}
	c.SetCookie("session_token", "", -1, "/", "", false, true)
	c.Redirect(http.StatusSeeOther, "/")
}

func (h *AuthHandler) ShowProfile(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, _ := wallRepo.GetPostsByUserID(user.ID)

	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, _ := socialRepo.GetByUserID(user.ID)

	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "Мой профиль",
		"NavActive":      "my_profile",
		"User":           user,
		"Posts":          posts,
		"SocialLinks":    socialLinks,
		"FollowersCount": followersCount,
		"FollowingCount": followingCount,
		"FriendsCount":   friendsCount,
		"CurrentUser":    user,
	})
}

// ---------- ANDROID/JSON API METHODS ----------

func (h *AuthHandler) LoginJSON(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Неверный email или пароль"})
		return
	}

	user, err := h.userRepo.GetUserByEmail(req.Email)
	if err != nil || !user.CheckPassword(req.Password) {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Неверный email или пароль"})
		return
	}

	token := uuid.New().String()
	session := &models.Session{
		UserID:    user.ID,
		Token:     token,
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	if err := h.sessionRepo.CreateSession(session); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Ошибка создания сессии"})
		return
	}

	c.SetCookie("session_token", token, 3600*24, "/", "", false, true)

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"user": gin.H{
			"id":         user.ID,
			"email":      user.Email,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"gender":     user.Gender,
			"age":        user.Age,
			"phone":      user.Phone,
		},
		"token": token,
	})
}

func (h *AuthHandler) ProfileJSON(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Не авторизован"})
		return
	}

	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, _ := wallRepo.GetPostsByUserID(user.ID)

	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, _ := socialRepo.GetByUserID(user.ID)

	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)

	c.JSON(http.StatusOK, gin.H{
		"user": gin.H{
			"id":         user.ID,
			"email":      user.Email,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"gender":     user.Gender,
			"age":        user.Age,
			"phone":      user.Phone,
		},
		"posts":          posts,
		"social_links":   socialLinks,
		"followers":      followersCount,
		"following":      followingCount,
		"friends_count":  friendsCount,
	})
}
// RegisterJSON — метод для Android/JSON регистрации
func (h *AuthHandler) RegisterJSON(c *gin.Context) {
    var req models.RegisterRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{
            "success": false,
            "message": "Некорректные данные регистрации",
        })
        return
    }

    // Проверяем, есть ли уже пользователь с таким email
    existingUser, _ := h.userRepo.GetUserByEmail(req.Email)
    if existingUser != nil {
        c.JSON(http.StatusConflict, gin.H{
            "success": false,
            "message": "Пользователь с таким email уже существует",
        })
        return
    }

    // Создаём нового пользователя
    user := &models.User{
        FirstName: req.FirstName,
        LastName:  req.LastName,
        Email:     req.Email,
        Password:  req.Password, // пока plain text
        Gender:    req.Gender,
        Age:       req.Age,
        Phone:     req.Phone,
    }

    // ХЕШИРУЕМ ПАРОЛЬ перед сохранением
    if err := user.HashPassword(req.Password); err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "success": false,
            "message": "Ошибка обработки пароля",
        })
        return
    }

    if err := h.userRepo.CreateUser(user); err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "success": false,
            "message": "Ошибка создания пользователя",
        })
        return
    }

    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "message": "Пользователь успешно создан",
        "user": gin.H{
            "id":         user.ID,
            "email":      user.Email,
            "first_name": user.FirstName,
            "last_name":  user.LastName,
            "gender":     user.Gender,
            "age":        user.Age,
            "phone":      user.Phone,
        },
    })
}