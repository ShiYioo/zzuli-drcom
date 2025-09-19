package org.shiyi.zzuli_drcom

import javafx.animation.*
import javafx.scene.Node
import javafx.scene.effect.DropShadow
import javafx.scene.effect.GaussianBlur
import javafx.scene.paint.Color
import javafx.util.Duration

/**
 * iOS风格动画工具类
 * 提供流畅的动画效果，为以后的更新做准备
 */
object IOSAnimations {

    // 标准iOS动画时长
    private val FAST_DURATION = Duration.millis(200.0)
    private val NORMAL_DURATION = Duration.millis(300.0)
    private val SLOW_DURATION = Duration.millis(500.0)

    // iOS风格的缓动函数
    private val EASE_OUT = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)
    private val EASE_IN_OUT = Interpolator.SPLINE(0.42, 0.0, 0.58, 1.0)
    private val SPRING = Interpolator.SPLINE(0.175, 0.885, 0.32, 1.275)

    /**
     * 淡入动画
     */
    fun fadeIn(node: Node, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        node.opacity = 0.0
        node.isVisible = true

        val timeline = Timeline(
            KeyFrame(duration, KeyValue(node.opacityProperty(), 1.0, EASE_OUT))
        )

        onFinished?.let { timeline.setOnFinished { it() } }
        timeline.play()
        return timeline
    }

    /**
     * 淡出动画
     */
    fun fadeOut(node: Node, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        val timeline = Timeline(
            KeyFrame(duration, KeyValue(node.opacityProperty(), 0.0, EASE_OUT))
        )

        timeline.setOnFinished {
            node.isVisible = false
            node.opacity = 1.0 // 重置透明度
            onFinished?.invoke()
        }
        timeline.play()
        return timeline
    }

    /**
     * 滑入动画（从下方滑入）
     */
    fun slideInFromBottom(node: Node, distance: Double = 50.0, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        node.opacity = 0.0
        node.translateY = distance
        node.isVisible = true

        val timeline = Timeline(
            KeyFrame(duration,
                KeyValue(node.opacityProperty(), 1.0, EASE_OUT),
                KeyValue(node.translateYProperty(), 0.0, SPRING)
            )
        )

        onFinished?.let { timeline.setOnFinished { it() } }
        timeline.play()
        return timeline
    }

    /**
     * 滑出动画（向下滑出）
     */
    fun slideOutToBottom(node: Node, distance: Double = 50.0, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        val timeline = Timeline(
            KeyFrame(duration,
                KeyValue(node.opacityProperty(), 0.0, EASE_OUT),
                KeyValue(node.translateYProperty(), distance, EASE_OUT)
            )
        )

        timeline.setOnFinished {
            node.isVisible = false
            node.opacity = 1.0
            node.translateY = 0.0
            onFinished?.invoke()
        }
        timeline.play()
        return timeline
    }

    /**
     * 滑入动画（从左侧滑入）
     */
    fun slideInFromLeft(node: Node, distance: Double = 300.0, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        node.opacity = 0.0
        node.translateX = -distance
        node.isVisible = true

        val timeline = Timeline(
            KeyFrame(duration,
                KeyValue(node.opacityProperty(), 1.0, EASE_OUT),
                KeyValue(node.translateXProperty(), 0.0, SPRING)
            )
        )

        onFinished?.let { timeline.setOnFinished { it() } }
        timeline.play()
        return timeline
    }

    /**
     * 滑出动画（向右滑出）
     */
    fun slideOutToRight(node: Node, distance: Double = 300.0, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        val timeline = Timeline(
            KeyFrame(duration,
                KeyValue(node.opacityProperty(), 0.0, EASE_OUT),
                KeyValue(node.translateXProperty(), distance, EASE_OUT)
            )
        )

        timeline.setOnFinished {
            node.isVisible = false
            node.opacity = 1.0
            node.translateX = 0.0
            onFinished?.invoke()
        }
        timeline.play()
        return timeline
    }

    /**
     * 弹性缩放动画
     */
    fun bounceScale(node: Node, fromScale: Double = 0.8, toScale: Double = 1.0, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        node.scaleX = fromScale
        node.scaleY = fromScale
        node.opacity = 0.0
        node.isVisible = true

        val timeline = Timeline(
            KeyFrame(duration,
                KeyValue(node.scaleXProperty(), toScale, SPRING),
                KeyValue(node.scaleYProperty(), toScale, SPRING),
                KeyValue(node.opacityProperty(), 1.0, EASE_OUT)
            )
        )

        onFinished?.let { timeline.setOnFinished { it() } }
        timeline.play()
        return timeline
    }

    /**
     * 缩放消失动画
     */
    fun scaleOut(node: Node, toScale: Double = 0.8, duration: Duration = FAST_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        val timeline = Timeline(
            KeyFrame(duration,
                KeyValue(node.scaleXProperty(), toScale, EASE_OUT),
                KeyValue(node.scaleYProperty(), toScale, EASE_OUT),
                KeyValue(node.opacityProperty(), 0.0, EASE_OUT)
            )
        )

        timeline.setOnFinished {
            node.isVisible = false
            node.scaleX = 1.0
            node.scaleY = 1.0
            node.opacity = 1.0
            onFinished?.invoke()
        }
        timeline.play()
        return timeline
    }

    /**
     * 按钮点击动画
     */
    fun buttonPressed(node: Node): Timeline {
        val timeline = Timeline(
            KeyFrame(Duration.millis(50.0),
                KeyValue(node.scaleXProperty(), 0.95, EASE_OUT),
                KeyValue(node.scaleYProperty(), 0.95, EASE_OUT)
            ),
            KeyFrame(Duration.millis(150.0),
                KeyValue(node.scaleXProperty(), 1.0, SPRING),
                KeyValue(node.scaleYProperty(), 1.0, SPRING)
            )
        )
        timeline.play()
        return timeline
    }

    /**
     * 摇摆动画（用于错误提示）
     */
    fun shake(node: Node, intensity: Double = 10.0): Timeline {
        val timeline = Timeline(
            KeyFrame(Duration.millis(0.0), KeyValue(node.translateXProperty(), 0.0)),
            KeyFrame(Duration.millis(50.0), KeyValue(node.translateXProperty(), intensity)),
            KeyFrame(Duration.millis(100.0), KeyValue(node.translateXProperty(), -intensity)),
            KeyFrame(Duration.millis(150.0), KeyValue(node.translateXProperty(), intensity * 0.7)),
            KeyFrame(Duration.millis(200.0), KeyValue(node.translateXProperty(), -intensity * 0.5)),
            KeyFrame(Duration.millis(250.0), KeyValue(node.translateXProperty(), intensity * 0.3)),
            KeyFrame(Duration.millis(300.0), KeyValue(node.translateXProperty(), 0.0))
        )
        timeline.play()
        return timeline
    }

    /**
     * 进度指示器旋转动画
     */
    fun spinProgress(node: Node): Timeline {
        val timeline = Timeline(
            KeyFrame(Duration.seconds(1.0),
                KeyValue(node.rotateProperty(), 360.0, Interpolator.LINEAR)
            )
        )
        timeline.cycleCount = Timeline.INDEFINITE
        timeline.play()
        return timeline
    }

    /**
     * 高级设置展开/收起动画
     */
    fun expandCollapse(node: Node, expand: Boolean, duration: Duration = NORMAL_DURATION, onFinished: (() -> Unit)? = null): Timeline {
        if (expand) {
            // 展开动画
            node.isVisible = true
            node.isManaged = true
            node.scaleY = 0.0
            node.opacity = 0.0

            val timeline = Timeline(
                KeyFrame(duration,
                    KeyValue(node.scaleYProperty(), 1.0, SPRING),
                    KeyValue(node.opacityProperty(), 1.0, EASE_OUT)
                )
            )
            onFinished?.let { timeline.setOnFinished { it() } }
            timeline.play()
            return timeline
        } else {
            // 收起动画
            val timeline = Timeline(
                KeyFrame(duration,
                    KeyValue(node.scaleYProperty(), 0.0, EASE_OUT),
                    KeyValue(node.opacityProperty(), 0.0, EASE_OUT)
                )
            )
            timeline.setOnFinished {
                node.isVisible = false
                node.isManaged = false
                node.scaleY = 1.0
                node.opacity = 1.0
                onFinished?.invoke()
            }
            timeline.play()
            return timeline
        }
    }

    /**
     * 状态切换动画（带模糊效果）
     */
    fun statusChange(node: Node, onFinished: (() -> Unit)? = null): Timeline {
        val blur = GaussianBlur(0.0)
        node.effect = blur

        val timeline = Timeline(
            KeyFrame(Duration.millis(150.0),
                KeyValue(blur.radiusProperty(), 3.0, EASE_OUT),
                KeyValue(node.opacityProperty(), 0.7, EASE_OUT)
            ),
            KeyFrame(Duration.millis(300.0),
                KeyValue(blur.radiusProperty(), 0.0, EASE_OUT),
                KeyValue(node.opacityProperty(), 1.0, EASE_OUT)
            )
        )

        timeline.setOnFinished {
            node.effect = null
            onFinished?.invoke()
        }
        timeline.play()
        return timeline
    }

    /**
     * 添加阴影效果
     */
    fun addShadow(node: Node, radius: Double = 10.0, offsetY: Double = 2.0, color: Color = Color.rgb(0, 0, 0, 0.1)) {
        val shadow = DropShadow()
        shadow.radius = radius
        shadow.offsetY = offsetY
        shadow.color = color
        node.effect = shadow
    }

    /**
     * 悬停动画
     */
    fun addHoverEffect(node: Node) {
        var hoverTimeline: Timeline? = null

        node.setOnMouseEntered {
            hoverTimeline?.stop()
            hoverTimeline = Timeline(
                KeyFrame(Duration.millis(200.0),
                    KeyValue(node.scaleXProperty(), 1.05, EASE_OUT),
                    KeyValue(node.scaleYProperty(), 1.05, EASE_OUT)
                )
            )
            hoverTimeline?.play()
        }

        node.setOnMouseExited {
            hoverTimeline?.stop()
            hoverTimeline = Timeline(
                KeyFrame(Duration.millis(200.0),
                    KeyValue(node.scaleXProperty(), 1.0, EASE_OUT),
                    KeyValue(node.scaleYProperty(), 1.0, EASE_OUT)
                )
            )
            hoverTimeline?.play()
        }
    }

    /**
     * 页面切换动画（从右滑入）
     */
    fun pageTransition(outNode: Node, inNode: Node, onFinished: (() -> Unit)? = null): Timeline {
        // 准备进入节点
        inNode.translateX = outNode.boundsInParent.width
        inNode.isVisible = true

        val timeline = Timeline(
            KeyFrame(Duration.millis(350.0),
                // 出去的节点向左滑出
                KeyValue(outNode.translateXProperty(), -outNode.boundsInParent.width, EASE_IN_OUT),
                KeyValue(outNode.opacityProperty(), 0.8, EASE_OUT),
                // 进入的节点从右滑入
                KeyValue(inNode.translateXProperty(), 0.0, EASE_IN_OUT),
                KeyValue(inNode.opacityProperty(), 1.0, EASE_OUT)
            )
        )

        timeline.setOnFinished {
            outNode.isVisible = false
            outNode.translateX = 0.0
            outNode.opacity = 1.0
            onFinished?.invoke()
        }
        timeline.play()
        return timeline
    }
}
