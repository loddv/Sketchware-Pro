package mod.loddv.dev.tools.offlineTranslator.translator;

import android.os.Parcel;
import android.os.Parcelable;

public enum ErrorType implements Parcelable {
    COULD_NOT_DETECT_LANGUAGE,
    DETECTED_BUT_UNAVAILABLE,
    UNEXPECTED;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name());
    }

    public static final Creator<ErrorType> CREATOR = new Creator<ErrorType>() {
        @Override
        public ErrorType createFromParcel(Parcel in) {
            try {
                return ErrorType.valueOf(in.readString());
            } catch (Exception e) {
                return UNEXPECTED;
            }
        }

        @Override
        public ErrorType[] newArray(int size) {
            return new ErrorType[size];
        }
    };
}
