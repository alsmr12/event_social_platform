package middleware

import (
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

// EventAccessMiddleware проверяет доступ к событию по ID
func EventAccessMiddleware(eventRepo *repository.EventRepository) gin.HandlerFunc {
    return func(c *gin.Context) {
        eventIDStr := c.Param("id")
        eventID, err := strconv.ParseUint(eventIDStr, 10, 32)
        if err != nil {
            c.HTML(http.StatusBadRequest, "base.html", gin.H{
                "Title": "Ошибка",
                "Error": "Неверный ID события",
            })
            c.Abort()
            return
        }

        currentUser := GetUserFromContext(c)
        if currentUser == nil {
            c.Redirect(http.StatusSeeOther, "/login")
            c.Abort()
            return
        }

        // Проверяем доступ
        hasAccess, err := eventRepo.CanUserAccessEvent(currentUser.ID, uint(eventID))
        if err != nil {
            c.HTML(http.StatusInternalServerError, "base.html", gin.H{
                "Title":       "Ошибка",
                "NavActive":   "events", 
                "Error":       "Ошибка при проверке доступа",
                "CurrentUser": currentUser,
            })
            c.Abort()
            return
        }

        if !hasAccess {
            c.HTML(http.StatusForbidden, "base.html", gin.H{
                "Title":       "Доступ запрещен",
                "NavActive":   "events", 
                "Error":       "У вас нет доступа к этому событию. Это приватное событие, доступное только по приглашению.",
                "CurrentUser": currentUser,
            })
            c.Abort()
            return
        }

        c.Next()
    }
}