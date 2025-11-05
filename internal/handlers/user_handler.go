package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
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
			"Error":     "Неверные данные формы",
		})
		return
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
		Age:       req.Age,
		Phone:     req.Phone,
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
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":     "Профиль",
			"NavActive": "profile", // ИСПРАВЛЕНО (было "profiles")
			"Error":     "Неверный ID профиля",
		})
		return
	}

	user, err := h.userRepo.GetUserByID(uint(id))
	if err != nil || user == nil {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":     "Профиль",
			"NavActive": "profile", // ИСПРАВЛЕНО (было "profiles")
			"Error":     "Профиль не найден",
		})
		return
	}

	// Получаем записи на стене пользователя
	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, err := wallRepo.GetPostsByUserID(uint(id))
	if err != nil {
		posts = []*models.WallPost{}
	}

	// Получаем социальные сети пользователя
	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, err := socialRepo.GetByUserID(uint(id))
	if err != nil {
		socialLinks = []*models.SocialLink{}
	}

	currentUser := GetUserFromContext(c)

	// Получаем статистику подписок
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(uint(id))
	followingCount, _ := subscriptionRepo.GetFollowingCount(uint(id))

	// Проверяем, подписан ли текущий пользователь на этого пользователя
	var isSubscribed bool
	if currentUser != nil {
		isSubscribed, _ = subscriptionRepo.IsSubscribed(currentUser.ID, uint(id))
	}

	// Получаем статус дружбы
	var friendshipStatus string = "none"
	var isIncomingRequest bool = false
	if currentUser != nil && currentUser.ID != uint(id) {
		friendshipRepo := repository.NewFriendshipRepository(db)
		friendshipStatus, err = friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(id))
		if err != nil {
			friendshipStatus = "none"
		}
		if friendshipStatus == "rejected" {
			friendshipStatus = "none"
		}

		// Проверяем, является ли запрос входящим
		if friendshipStatus == "pending" {
			var friendship models.Friendship
			err := db.Where("user_id = ? AND friend_id = ?", uint(id), currentUser.ID).First(&friendship).Error
			isIncomingRequest = (err == nil)
		}
	}

	// Получаем количество друзей
	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(uint(id))

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":             "Профиль " + user.FirstName + " " + user.LastName,
		"NavActive":         "profile",
		"CurrentUser":       currentUser,
		"User":              user,
		"Posts":             posts,
		"SocialLinks":       socialLinks,
		"FollowersCount":    followersCount,
		"FollowingCount":    followingCount,
		"FriendsCount":      friendsCount,
		"IsSubscribed":      isSubscribed,
		"FriendshipStatus":  friendshipStatus,
		"IsIncomingRequest": isIncomingRequest,
	})
}

func (h *UserHandler) GetAllProfiles(c *gin.Context) {
	users, err := h.userRepo.GetAllUsers()
	if err != nil {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":       "Все пользователи",
			"NavActive":   "profiles", // УЖЕ ПРАВИЛЬНО
			"Users":       []*models.User{},
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	if users == nil {
		users = []*models.User{}
	}

	currentUser := GetUserFromContext(c)

	// Создаем структуру для передачи дополнительных данных
	type UserWithSubscriptions struct {
		*models.User
		FollowersCount   int64
		FollowingCount   int64
		IsSubscribed     bool
		FriendshipStatus string
	}

	usersWithSubs := make([]UserWithSubscriptions, len(users))

	// Получаем репозитории
	db := h.userRepo.GetDB()
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	friendshipRepo := repository.NewFriendshipRepository(db)

	for i, user := range users {
		// Получаем статистику подписок для каждого пользователя
		followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
		followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

		// Проверяем, подписан ли текущий пользователь
		var isSubscribed bool
		var friendshipStatus string = "none"
		if currentUser != nil && currentUser.ID != user.ID {
			isSubscribed, _ = subscriptionRepo.IsSubscribed(currentUser.ID, user.ID)
			friendshipStatus, _ = friendshipRepo.GetFriendshipStatus(currentUser.ID, user.ID)
		}

		usersWithSubs[i] = UserWithSubscriptions{
			User:             user,
			FollowersCount:   followersCount,
			FollowingCount:   followingCount,
			IsSubscribed:     isSubscribed,
			FriendshipStatus: friendshipStatus,
		}
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Все пользователи",
		"NavActive":   "profiles",
		"Users":       usersWithSubs,
		"CurrentUser": currentUser,
	})
}

func (h *UserHandler) ShowEditProfileForm(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	db := h.userRepo.GetDB()
	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, err := socialRepo.GetByUserID(currentUser.ID)
	if err != nil {
		socialLinks = []*models.SocialLink{}
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Редактирование профиля",
		"NavActive":   "edit_profile",
		"User":        currentUser,
		"CurrentUser": currentUser,
		"SocialLinks": socialLinks,
	})
}

func (h *UserHandler) UpdateProfile(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.UpdateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Редактирование профиля",
			"NavActive":   "profile",
			"Error":       "Неверные данные формы: " + err.Error(),
			"User":        currentUser,
			"CurrentUser": currentUser,
		})
		return
	}

	// Обновляем данные пользователя
	currentUser.FirstName = req.FirstName
	currentUser.LastName = req.LastName
	currentUser.Gender = req.Gender
	currentUser.Age = req.Age
	currentUser.Phone = req.Phone
	//currentUser.SocialLinks = req.SocialLinks

	if err := h.userRepo.UpdateUser(currentUser); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Редактирование профиля",
			"NavActive":   "profile",
			"Error":       "Ошибка обновления профиля: " + err.Error(),
			"User":        currentUser,
			"CurrentUser": currentUser,
		})
		return
	}

	db := h.userRepo.GetDB()
	socialRepo := repository.NewSocialLinkRepository(db)

	// Удаляем старые соцсети
	if err := socialRepo.DeleteByUserID(currentUser.ID); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Редактирование профиля",
			"NavActive":   "profile",
			"Error":       "Ошибка обновления социальных сетей: " + err.Error(),
			"User":        currentUser,
			"CurrentUser": currentUser,
		})
		return
	}

	// Сохраняем новые соцсети
	platforms := c.PostFormArray("platform[]")
	usernames := c.PostFormArray("username[]")
	customNames := c.PostFormArray("custom_name[]")

	for i, platform := range platforms {
		if platform != "" && i < len(usernames) && usernames[i] != "" {
			socialLink := &models.SocialLink{
				UserID:     currentUser.ID,
				Platform:   platform,
				Username:   usernames[i],
				CustomName: customNames[i],
			}
			if err := socialRepo.Create(socialLink); err != nil {
				c.HTML(http.StatusInternalServerError, "base.html", gin.H{
					"Title":       "Редактирование профиля",
					"NavActive":   "profile",
					"Error":       "Ошибка сохранения социальных сетей: " + err.Error(),
					"User":        currentUser,
					"CurrentUser": currentUser,
				})
				return
			}
		}
	}
	// === КОНЕЦ ОБРАБОТКИ СОЦСЕТЕЙ ===

	c.Redirect(http.StatusSeeOther, "/profile")
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

    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "data": users,
    })
}