package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ContributionGraph(
    contributions: Map<String, Int>, // dateKey -> minutes (yyyy-MM-dd)
    modifier: Modifier = Modifier,
    weeksToShow: Int = 12
) {
    val calendar = Calendar.getInstance()
    // Go to the start of the current week (Sunday)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    // Go back weeksToShow weeks
    calendar.add(Calendar.WEEK_OF_YEAR, -weeksToShow)
    
    val startDate = calendar.time.time // Save the start timestamp

    Column(modifier = modifier) {
        Text(
            text = "DAILY ACTIVITY TRACKER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Day of week labels
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 20.dp)
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").filterIndexed { i, _ -> i % 2 == 1 }.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(10.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // The grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
                
                repeat(weeksToShow + 1) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val weekCalendar = Calendar.getInstance()
                        weekCalendar.timeInMillis = startDate
                        weekCalendar.add(Calendar.WEEK_OF_YEAR, week)
                        
                        // Month label
                        if (week == 0 || weekCalendar.get(Calendar.DAY_OF_MONTH) <= 7) {
                            Text(
                                text = monthSdf.format(weekCalendar.time).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        repeat(7) { day ->
                            val dayCalendar = Calendar.getInstance()
                            dayCalendar.timeInMillis = weekCalendar.timeInMillis
                            dayCalendar.add(Calendar.DAY_OF_YEAR, day)
                            
                            val dateKey = sdf.format(dayCalendar.time)
                            val minutes = contributions[dateKey] ?: 0
                            
                            ContributionCell(minutes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionCell(minutes: Int) {
    val color = when {
        minutes <= 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        minutes < 15 -> Color(0xFFC6E48B)
        minutes < 30 -> Color(0xFF7BC96F)
        minutes < 45 -> Color(0xFF239A3B)
        else -> Color(0xFF196127)
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
