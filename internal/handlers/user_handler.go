package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
	"strconv"
	"time"
)

type UserHandler struct {
	userRepo *repository.UserRepository
}

func NewUserHandler(userRepo *repository.UserRepository) *UserHandler {
	return &UserHandler{userRepo: userRepo}
}

func (h *UserHandler) ShowHomePage(c *gin.Context) {
	currentUser := GetUserFromContext(c)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Главная",
		"NavActive":   "home",
		"CurrentUser": currentUser,
	})
}

func (h *UserHandler) ShowCreateProfileForm(c *gin.Context) {
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":     "Регистрация",
		"NavActive": "register",
	})
}

func (h *UserHandler) CreateProfile(c *gin.Context) {
	var req models.CreateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Неверные данные формы: " + err.Error(),
		})
		return
	}

	// Конвертация строки даты в time.Time
	var birthDate time.Time
	if req.BirthDate != "" {
		var err error
		birthDate, err = time.Parse("2006-01-02", req.BirthDate)
		if err != nil {
			c.HTML(http.StatusBadRequest, "base.html", gin.H{
				"Title":     "Регистрация",
				"NavActive": "register",
				"Error":     "Неверный формат даты рождения: " + err.Error(),
			})
			return
		}
	}

	if h.userRepo.UserExists(req.Email) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Пользователь с таким email уже существует",
		})
		return
	}

	user := &models.User{
		Email:     req.Email,
		FirstName: req.FirstName,
		LastName:  req.LastName,
		Gender:    req.Gender,
		BirthDate: birthDate,
		Phone:     req.Phone,
		City:      req.City,
		Latitude:  req.Latitude,
		Longitude: req.Longitude,
		//SocialLinks: req.SocialLinks,
	}

	if err := user.HashPassword(req.Password); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Ошибка при хешировании пароля: " + err.Error(),
		})
		return
	}

	if err := h.userRepo.CreateUser(user); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Ошибка при создании профиля: " + err.Error(),
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/login")
}

func (h *UserHandler) GetProfile(c *gin.Context) {
	// Получаем ID профиля из параметра
	profileID := c.Param("id")

	// Получаем текущего пользователя
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Определяем, является ли профиль своим
	isOwnProfile := profileID == "" || (currentUser != nil && (profileID == strconv.Itoa(int(currentUser.ID)) || profileID == "self" || profileID == "my"))

	// Если это свой профиль, устанавливаем profileID в пустую строку для корректной работы шаблона
	if isOwnProfile {
		profileID = ""
	}

	// Передаем данные в шаблон
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":        "Профиль",
		"NavActive":    "my_profile",
		"CurrentUser":  currentUser,
		"ProfileID":    profileID,
		"IsOwnProfile": isOwnProfile,
	})
}

func (h *UserHandler) ShowEditProfileForm(c *gin.Context) {
	// Получаем текущего пользователя
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем социальные сети пользователя
	db := h.userRepo.GetDB()
	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, err := socialRepo.GetByUserID(currentUser.ID)
	if err != nil {
		socialLinks = []*models.SocialLink{}
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Редактирование профиля",
		"NavActive":   "edit_profile",
		"CurrentUser": currentUser,
		"SocialLinks": socialLinks,
	})
}

func (h *UserHandler) UpdateProfile(c *gin.Context) {
	// Перенаправляем на использование JSON API метода
	// Это позволяет избежать дублирования кода и использовать единый метод обновления профиля
	h.UpdateProfileJSON(c)
}

func (h *UserHandler) GetAllProfilesJSON(c *gin.Context) {
	users, err := h.userRepo.GetAllUsers()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения пользователей",
		})
		return
	}

	// Добавляем статистику для каждого пользователя
	for _, user := range users {
		// Получаем количество подписчиков
		subscriptionRepo := repository.NewSubscriptionRepository(h.userRepo.GetDB())
		followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
		user.FollowersCount = int(followersCount)

		// Получаем количество подписок
		followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)
		user.FollowingCount = int(followingCount)

		// Получаем количество друзей
		friendshipRepo := repository.NewFriendshipRepository(h.userRepo.GetDB())
		friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)
		user.FriendsCount = int(friendsCount)

		// Получаем социальные сети
		socialRepo := repository.NewSocialLinkRepository(h.userRepo.GetDB())
		socialLinks, _ := socialRepo.GetByUserID(user.ID)
		user.SocialLinks = socialLinks

		// Получаем последние посты
		wallRepo := repository.NewWallRepository(h.userRepo.GetDB())
		posts, _ := wallRepo.GetPostsByUserID(user.ID)
		user.Posts = posts
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"data":    users,
	})
}

// ========== ANDROID/JSON API METHODS ==========

// UpdateProfileJSON - обновление профиля пользователя
func (h *UserHandler) UpdateProfileJSON(c *gin.Context) {
	log.Printf("🔍 UpdateProfileJSON called")

	// Получаем текущего пользователя
	user := GetUserFromContext(c)
	if user == nil {
		log.Printf("❌ UpdateProfileJSON: User not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	log.Printf("✅ UpdateProfileJSON: Updating user %s (ID: %d)", user.Email, user.ID)

	var req struct {
		FirstName   string               `json:"first_name" binding:"required"`
		LastName    string               `json:"last_name" binding:"required"`
		Gender      string               `json:"gender"`
		BirthDate   string               `json:"birth_date"`
		Phone       string               `json:"phone"`
		SocialLinks []*models.SocialLink `json:"social_links"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		log.Printf("❌ UpdateProfileJSON: Invalid request data: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверные данные: " + err.Error(),
		})
		return
	}

	// Обновляем данные пользователя
	user.FirstName = req.FirstName
	user.LastName = req.LastName
	user.Gender = req.Gender
	user.Phone = req.Phone

	// Обрабатываем дату рождения
	if req.BirthDate != "" {
		birthDate, err := time.Parse("2006-01-02", req.BirthDate)
		if err != nil {
			log.Printf("❌ UpdateProfileJSON: Invalid birth date: %v", err)
			c.JSON(http.StatusBadRequest, gin.H{
				"success": false,
				"message": "Неверный формат даты рождения",
			})
			return
		}
		user.BirthDate = birthDate
	} else {
		// Если дата рождения не передана, устанавливаем нулевую дату
		user.BirthDate = time.Time{}
	}

	if err := h.userRepo.UpdateUser(user); err != nil {
		log.Printf("❌ UpdateProfileJSON: Error updating user: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка обновления профиля: " + err.Error(),
		})
		return
	}

	log.Printf("✅ UpdateProfileJSON: Profile updated successfully for user %s", user.Email)

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Профиль успешно обновлен",
		"user": gin.H{
			"id":         user.ID,
			"email":      user.Email,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"gender":     user.Gender,
			"birth_date": user.BirthDate.Format("2006-01-02"),
			"age":        user.GetAge(), // Вычисляем возраст на лету
			"phone":      user.Phone,
		},
	})
}

// GetUserStatsJSON - получение статистики пользователя
func (h *UserHandler) GetUserStatsJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	db := h.userRepo.GetDB()

	// Получаем количество друзей
	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(currentUser.ID)

	// Получаем количество подписчиков и подписок
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(currentUser.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(currentUser.ID)

	// Получаем количество событий пользователя
	eventRepo := repository.NewEventRepository(db)
	userEventsCount, _ := eventRepo.GetUserEventsCountAndroid(currentUser.ID)

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"stats": gin.H{
			"friends_count":   friendsCount,
			"followers_count": followersCount,
			"following_count": followingCount,
			"events_count":    userEventsCount,
		},
	})
}
