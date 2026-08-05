package app.organicmaps.chat.realtime;

/** Connection states exposed to chat UI and background sync. */
public enum ChatConnectionState
{
  offline, connecting, connected, reconnecting, background, failed
}
