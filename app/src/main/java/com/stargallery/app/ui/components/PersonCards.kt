package com.stargallery.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.util.ImageKind

/** Portrait card (2:3) with name overlay; used in grids and carousels. */
@Composable
fun PersonCard(
    person: PersonSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = person.knownFor.firstOrNull() ?: person.knownForDepartment
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClickLabel = "Open profile of ${person.name}", role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${person.name}${subtitle?.let { ", $it" } ?: ""}"
            }
    ) {
        RemoteImage(
            path = person.profilePath,
            kind = ImageKind.PROFILE,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .bottomScrim()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                person.name,
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = com.stargallery.app.ui.theme.SGColors.CoolGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PersonCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(18.dp))
            .shimmer()
    )
}

@Composable
fun PersonCarousel(
    people: List<PersonSummary>,
    onPersonClick: (PersonSummary) -> Unit,
    cardWidth: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(people, key = { it.id }) { person ->
            PersonCard(person, onClick = { onPersonClick(person) }, modifier = Modifier.width(cardWidth))
        }
    }
}

@Composable
fun CarouselSkeleton(cardWidth: Dp = 140.dp, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) { PersonCardSkeleton(Modifier.width(cardWidth)) }
    }
}
