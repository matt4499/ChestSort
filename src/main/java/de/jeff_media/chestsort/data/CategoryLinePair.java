package de.jeff_media.chestsort.data;

import de.jeff_media.chestsort.utils.Utils;

public record CategoryLinePair(String categoryName, short position, boolean sticky) {

    public CategoryLinePair(String categoryName, short position) {
        this(categoryName, position, false);
    }

    public String formattedPosition() {
        return Utils.shortToStringWithLeadingZeroes(position);
    }

    public String categoryNameSticky() {
        return sticky ? categoryName + "~" + formattedPosition() : categoryName;
    }
}
