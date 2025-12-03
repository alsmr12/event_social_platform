package handlers

import (
	"event_social_platform/internal/repository"
	"net/http"

	"github.com/gin-gonic/gin"
)

type AchievementHandler struct {
	achievementRepo *repository.AchievementRepository
}

func NewAchievementHandler(achievementRepo *repository.AchievementRepository) *AchievementHandler {
	return &AchievementHandler{
		achievementRepo: achievementRepo,
	}
}

// Страница рейтинга
func (h *AchievementHandler) ShowRatings(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	search := c.Query("search")
	ratings, err := h.achievementRepo.GetUserRatings(search)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Рейтинг пользователей",
			"NavActive":   "ratings",
			"Error":       "Ошибка загрузки рейтинга",
			"CurrentUser": currentUser,
		})
		return
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Рейтинг пользователей",
		"NavActive":   "ratings",
		"Ratings":     ratings,
		"Search":      search,
		"CurrentUser": currentUser,
	})
}

// Страница моих наград
func (h *AchievementHandler) ShowMyAchievements(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	achievements, err := h.achievementRepo.GetUserAchievements(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Мои награды",
			"NavActive":   "my_achievements",
			"Error":       "Ошибка загрузки наград",
			"CurrentUser": currentUser,
		})
		return
	}

	totalPoints, _ := h.achievementRepo.GetUserTotalPoints(currentUser.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":        "Мои награды",
		"NavActive":    "my_achievements",
		"Achievements": achievements,
		"TotalPoints":  totalPoints,
		"CurrentUser":  currentUser,
	})
}

// Обновить прогресс текущего пользователя
func (h *AchievementHandler) UpdateProgress(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	if err := h.achievementRepo.UpdateAllUsersProgress(); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Мои награды",
			"NavActive":   "my_achievements",
			"Error":       "Ошибка обновления прогресса",
			"CurrentUser": currentUser,
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/my-achievements?message=updated")
}
// Получить мои достижения (JSON)
func (h *AchievementHandler) GetMyAchievementsJSON(c *gin.Context) {
    currentUser := GetUserFromContext(c)
    if currentUser == nil {
        c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Не авторизован"})
        return
    }

    achievements, err := h.achievementRepo.GetUserAchievements(currentUser.ID)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Ошибка загрузки достижений"})
        return
    }

    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "achievements": achievements,
    })
}

// Получить рейтинг (JSON)
func (h *AchievementHandler) GetRatingsJSON(c *gin.Context) {
    search := c.Query("search")
    ratings, err := h.achievementRepo.GetUserRatings(search)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Ошибка загрузки рейтинга"})
        return
    }

    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "ratings": ratings,
    })
}

// Получить общее количество очков (JSON)
func (h *AchievementHandler) GetTotalPointsJSON(c *gin.Context) {
    currentUser := GetUserFromContext(c)
    if currentUser == nil {
        c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Не авторизован"})
        return
    }

    points, err := h.achievementRepo.GetUserTotalPoints(currentUser.ID)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Ошибка загрузки очков"})
        return
    }

    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "points": points,
    })
}