package app.organicmaps.widget.placepage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import app.organicmaps.sdk.bookmarks.data.Bookmark;
import app.organicmaps.sdk.bookmarks.data.ElevationInfo;
import app.organicmaps.sdk.bookmarks.data.MapObject;
import app.organicmaps.sdk.bookmarks.data.Track;
import java.util.List;
import app.organicmaps.widget.placepage.online.FoursquarePlacesRepository;
import app.organicmaps.widget.placepage.online.OnlinePlaceDetails;

public class PlacePageViewModel extends ViewModel
{
  private final MutableLiveData<List<PlacePageButtons.ButtonType>> mCurrentButtons = new MutableLiveData<>();
  private final MutableLiveData<MapObject> mMapObject = new MutableLiveData<>();
  private final MutableLiveData<Integer> mPlacePageWidth = new MutableLiveData<>();
  private final MutableLiveData<Integer> mPlacePageDistanceToTop = new MutableLiveData<>();
  private final MutableLiveData<OnlinePlaceDetails> mOnlinePlaceDetails = new MutableLiveData<>();
  private final FoursquarePlacesRepository mFoursquare = new FoursquarePlacesRepository(BuildConfig.FOURSQUARE_API_KEY);
  public boolean isAlertDialogShowing = false;

  public LiveData<OnlinePlaceDetails> getOnlinePlaceDetails() { return mOnlinePlaceDetails; }

  /** Loads provider details for the currently selected place; results are session-only. */
  public void loadOnlinePlaceDetails(MapObject object)
  {
    if (object == null || object.isTrack() || object.isTrackRecording()) return;
    mFoursquare.findNearby(object.getLat(), object.getLon(), object.getName(), new FoursquarePlacesRepository.Callback()
    {
      @Override public void onLoaded(OnlinePlaceDetails details) { mOnlinePlaceDetails.postValue(details); }
      @Override public void onUnavailable() { mOnlinePlaceDetails.postValue(null); }
    });
  }

  public LiveData<List<PlacePageButtons.ButtonType>> getCurrentButtons()
  {
    return mCurrentButtons;
  }

  public void setCurrentButtons(List<PlacePageButtons.ButtonType> buttons)
  {
    mCurrentButtons.setValue(buttons);
  }

  public LiveData<MapObject> getMapObject()
  {
    return mMapObject;
  }

  public void setMapObject(MapObject mapObject)
  {
    mMapObject.setValue(mapObject);
    mOnlinePlaceDetails.setValue(null);
    if (mapObject != null)
      loadOnlinePlaceDetails(mapObject);
  }

  public LiveData<Integer> getPlacePageWidth()
  {
    return mPlacePageWidth;
  }

  public void setPlacePageWidth(int width)
  {
    mPlacePageWidth.setValue(width);
  }

  public LiveData<Integer> getPlacePageDistanceToTop()
  {
    return mPlacePageDistanceToTop;
  }

  public void setPlacePageDistanceToTop(int top)
  {
    mPlacePageDistanceToTop.setValue(top);
  }
}
