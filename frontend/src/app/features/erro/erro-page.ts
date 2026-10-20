import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonDirective } from 'primeng/button';

@Component({
  selector: 'app-erro-page',
  imports: [RouterLink, ButtonDirective],
  templateUrl: './erro-page.html',
  styleUrl: './erro-page.scss',
})
export class ErroPage {}
