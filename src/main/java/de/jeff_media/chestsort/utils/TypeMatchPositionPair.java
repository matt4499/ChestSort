package de.jeff_media.chestsort.utils;

public record TypeMatchPositionPair(String typeMatch, short position, boolean sticky) {

    public TypeMatchPositionPair(String typeMatch, short position) {
        this(typeMatch, position, false);
    }

    public String formattedPosition() {
        return Utils.shortToStringWithLeadingZeroes(position);
    }
}
