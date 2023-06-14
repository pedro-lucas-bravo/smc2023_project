


waxml.addEventListener("inited", () => {

  document.querySelector("navigation > #playMusicBtn").addEventListener("click", e => {
    if(!iMusic.isPlaying()){
      iMusic.play();
    }
    if(e.target.checked){
      waxml.unmute();
    } else {
      waxml.mute();
    }
  });
  waxml.mute();

});

